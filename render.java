// Scholz, Carsten, 11218504, Assignment number 2, 159.235
/* Draws 3 hand models, which can be either drawn in wireframe or filled in. view can rotate and move.
 * Modified version of MyProg29 to allow duplicating of model.*/
import java.awt.*;      
import java.awt.geom.*; 
import java.awt.event.*;
import javax.swing.*;   
import javax.swing.event.*;
import java.util.*;
import java.io.*;

// An adaptation of that Prog29 draws 3 extra hands
public class render extends JFrame implements ActionListener{

    private static final int DISPLAY_WIDTH   = 800;
    private static final int DISPLAY_HEIGHT  = 700;
    private static final int CONTROL_WIDTH   = 384;
    private static final int CONTROL_HEIGHT  = 384;
    private static final int FRAMES_PER_SEC  = 5;

    private Container con  = null;
    private DisplayArea da = null;
    ControlPanel cp        = null;

    JDialog fd             = null;   // the dialog is a  separate frame-like thing 
                                     // into which we can put the control panel

    public JButton toggleDialogButton = null;
    public JRadioButton drawNormalsButton  = null;
    public JRadioButton drawSurfacesButton = null;
    public JRadioButton drawWiresButton    = null;
    public JRadioButton antiAliasingButton = null;
    public JRadioButton drawAxesButton = null;
    public JMenuBar menuBar           = null;

    public static boolean drawAxes  = true;
    public static boolean drawNormals  = false;
    public static boolean drawSurfaces = false;
    public static boolean drawWires    = true;
    public static boolean antiAliasing = false;

    public static boolean showDialog = false;
    public static boolean rotatingX = false;
    public static boolean rotatingY = false;
    public static boolean rotatingZ = false;

    public static String filename = "hand.dat";  // default file to load

    public void actionPerformed( ActionEvent ev ){  // handle the master controls
	if( ev.getSource() == toggleDialogButton ){
	    showDialog = ! showDialog;
	    fd.setVisible( showDialog );
	}
	else if( ev.getSource() == drawNormalsButton ){
	    drawNormals = drawNormalsButton.isSelected();
	    repaint();
	}
	else if( ev.getSource() == drawSurfacesButton ){
	    drawSurfaces = drawSurfacesButton.isSelected();
	    repaint();
	}
	else if( ev.getSource() == drawWiresButton ){
	    drawWires = drawWiresButton.isSelected();
	    repaint();
	}
	else if( ev.getSource() == antiAliasingButton ){
	    antiAliasing = antiAliasingButton.isSelected();
	    repaint();
	}
	else if( ev.getSource() == drawAxesButton ){
	    drawAxes = drawAxesButton.isSelected();
	    repaint();
	}
    }

    public static void main( String args[] ){
	if( args.length != 0 ) filename = args[0];
	System.out.println( "----------------------------------------" );
	System.out.println( " 159.235 Assignment 2 Semester 2 2012  " );
	System.out.println( " Submitted by: Scholz, Carsten,  11218504 " );
	System.out.println( "----------------------------------------" );
	new render();
   }

    public render(){
	super("3D Rendering from File with Rotations & Controls & Back Face Culling");
	setDefaultCloseOperation(EXIT_ON_CLOSE);
	setResizable(false);

	menuBar = new JMenuBar();  // somewhere to put the master controls
	setJMenuBar(menuBar);
	toggleDialogButton = new JButton("Toggle Controls");
	toggleDialogButton.addActionListener( this );
	menuBar.add( toggleDialogButton );

	drawNormalsButton = new JRadioButton("Draw Normals");
	drawNormalsButton.addActionListener( this );
	drawNormalsButton.setSelected( drawNormals );
	menuBar.add( drawNormalsButton );

	drawSurfacesButton = new JRadioButton("Draw Surfaces");
	drawSurfacesButton.addActionListener( this );
	drawSurfacesButton.setSelected( drawSurfaces );
	menuBar.add( drawSurfacesButton );

	drawWiresButton = new JRadioButton("Draw Wires");
	drawWiresButton.addActionListener( this );
	drawWiresButton.setSelected( drawWires );
	menuBar.add( drawWiresButton );

	drawAxesButton = new JRadioButton("Draw Axes");
	drawAxesButton.addActionListener( this );
	drawAxesButton.setSelected( drawAxes );
	menuBar.add( drawAxesButton );

	antiAliasingButton = new JRadioButton("Anti Aliasing");
	antiAliasingButton.addActionListener( this );
	antiAliasingButton.setSelected( antiAliasing );
	menuBar.add( antiAliasingButton );

	con = getContentPane();
	con.setLayout( null );

	loadTriangles( filename );

	da = new DisplayArea( new Rectangle( 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT) );
	con.add( da );

	cp = new ControlPanel( new Rectangle( 0, 0, CONTROL_WIDTH, CONTROL_HEIGHT ) );
					      
	setVisible(true);
      	resizeToInternalSize( DISPLAY_WIDTH, DISPLAY_HEIGHT );

	fd = new JDialog(this, "Control Panel", false);
	fd.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	fd.setResizable(false);
	fd.getContentPane().add( cp );
	fd.setSize( CONTROL_WIDTH, CONTROL_HEIGHT );
	fd.setVisible(showDialog);

	long delayTime = 1000/FRAMES_PER_SEC;     // main loop
	long startTime, waitTime, elapsedTime;
	while( true ){
	    if( rotatingX || rotatingY || rotatingZ ){
		startTime = System.currentTimeMillis();

		if( rotatingX ) rotateX();
		if( rotatingY ) rotateY();
		if( rotatingZ ) rotateZ();

		elapsedTime = System.currentTimeMillis() - startTime;
		waitTime = Math.max( delayTime - elapsedTime, 5 );
		try{
		    Thread.sleep( waitTime );
		}catch( InterruptedException ie ){}
		da.repaint();
	    }
	}
    }

    public void resizeToInternalSize( int internalWidth, int internalHeight ){
	Insets insets = getInsets();
	final int newWidth  = internalWidth  + insets.left + insets.right;
	final int newHeight = internalHeight + insets.top  + insets.bottom;
	Runnable resize = new Runnable(){ // an anonymous inner class
		public void run(){
		    setSize( newWidth, newHeight);
		}
	};
	if(!SwingUtilities.isEventDispatchThread() ){
	    try{
		SwingUtilities.invokeAndWait( resize );
	    }catch( Exception e ) { }
	}
	else{
	    resize.run();
	}
	validate();
    }

    public class Triangle{
	int i1, i2, i3;
	Triangle( int j1, int j2, int j3 ){ i1=j1; i2=j2; i3=j3; }

	public Point3D centroid(){
	    Point3D c = new Point3D();
	    c.x = ( points[i1].x + points[i2].x + points[i3].x ) / 3.0;
	    c.y = ( points[i1].y + points[i2].y + points[i3].y ) / 3.0;
	    c.z = ( points[i1].z + points[i2].z + points[i3].z ) / 3.0;
	    return c;
	}

	public Point3D normal(){
	    Point3D p1 = points[i1];
	    Point3D p2 = points[i2];
	    Point3D p3 = points[i3];
	    Point3D v1 = p1.minus( p2 );
	    Point3D v2 = p2.minus( p3 );
	    Point3D n = v1.crossProduct(v2);
	    double scale = n.magnitude();
	    n.x /= scale;
	    n.y /= scale;
	    n.z /= scale;
	    return n;
	}
    }

    int randNum1=0;
    int randNum2=5;
    public Point3D points[];
    public Triangle[] triangles; 
    public Random rand = new Random();

    public void drawTriangles( Graphics2D g2 ){
	g2.setColor( Color.red );
	for(int j=0; j < 3; j++){
	for( int i = 0;i< triangles.length;i++){
	    try{
	    draw3DLine( g2, points[ triangles[i].i1 ], points[ triangles[i].i2 ],j );
	    draw3DLine( g2, points[ triangles[i].i2 ], points[ triangles[i].i3 ],j );
	    draw3DLine( g2, points[ triangles[i].i3 ], points[ triangles[i].i1 ],j );
	    }catch( ArrayIndexOutOfBoundsException e ){
		System.out.println( "out of bounds i = " + i );
	    }
	}
	}
    }

    public void normedTriangles( Graphics2D g2 ){
	g2.setColor( Color.blue );
	for(int j=0; j < 2; j++){
	for( int i = 0;i< triangles.length;i++){
	    try{
		Point3D c = triangles[i].centroid();
		Point2D p = c.projectPoint();
		Point2D n = c.add( triangles[i].normal().scale( 0.1 )  ).projectPoint();

		g2.drawLine( p.x, p.y, n.x, n.y );  // draw normals from the centroids outwards

		g2.drawOval( p.x, p.y, 2, 2 );    // draw dots at the centroids

	    }catch( ArrayIndexOutOfBoundsException e ){
		System.out.println( "out of bounds i = " + i );
	    }
	}
	}
    }

    public void fillTriangles( Graphics2D g2 ){  // fills *all* the triangles  a flat shade of green
	g2.setColor( Color.green );
	
	Point2D p;
	int[] xpoints = new int[3];
	int[] ypoints = new int[3];
	for(int j=0; j < 2; j++){
	for( int i = 0;i< triangles.length;i++){
	    try{
		p =  points[ triangles[i].i1 ].projectPoint();
		xpoints[0] = p.x;
		ypoints[0] = p.y;
		p =  points[ triangles[i].i2 ].projectPoint();
		xpoints[1] = p.x;
		ypoints[1] = p.y;
		p =  points[ triangles[i].i3 ].projectPoint();
		xpoints[2] = p.x;
		ypoints[2] = p.y;
		Polygon poly = new Polygon(xpoints, ypoints, 3 );
		g2.fill( poly );
	    }catch( ArrayIndexOutOfBoundsException e ){
		System.out.println( "out of bounds i = " + i );
	    }
	}
	}
    }

    public void fillSomeTriangles( Graphics2D g2 ){  // fills *forwards facing* triangles 

	float hue, saturation, brightness, red;
	float hsbvals[] = Color.RGBtoHSB( 0, 255, 0, null );  // an HSB  version of "Green"
	hue = hsbvals[0];
	saturation = hsbvals[1];
	brightness = hsbvals[2];

	Point2D p;
	int[] xpoints = new int[3];
	int[] ypoints = new int[3];

	double dot;

	Point3D light = new Point3D( 0, -1, 0 );  // shine a light from the viewer - ie  down the -y axis
	// the light and the viewer are the same vector for this set up.

	// for simple depth y-buffering we would need to  start from +y and work -ve in y - need to sort  accordingly
	// we  could use a permute-vector or array to sort them and  step through the back-sorted list
	for(int j=0; j < 3; j++){
		for( int i = 0;i< triangles.length;i++){
	    if( (dot = triangles[i].normal().dotProduct( light )) > 0.0 ){
		brightness = (float)dot;
		g2.setColor( Color.getHSBColor( hue, saturation, brightness ) );
		int temp1;
		int temp2;
		if(j == 0){
			temp1 = 0;
			temp2 = 0;
		}else if(j ==1){
			temp1 = 250;
			temp2 = 250;
		}else{
			temp1 = -250;
			temp2 = 250;
		}
		try{
		    p =  points[ triangles[i].i1 ].projectPoint();
		    xpoints[0] = p.x +temp1;
		    ypoints[0] = p.y +temp2;
		    p =  points[ triangles[i].i2 ].projectPoint();
		    xpoints[1] = p.x +temp1;
		    ypoints[1] = p.y +temp2;
		    p =  points[ triangles[i].i3 ].projectPoint();
		    xpoints[2] = p.x +temp1;
		    ypoints[2] = p.y +temp2;
		    Polygon poly = new Polygon(xpoints, ypoints, 3 );
		    g2.fill( poly );
			
		}catch( ArrayIndexOutOfBoundsException e ){
		    System.out.println( "out of bounds i = " + i );
		}
	    }
	}
	}
    }

    // read  list of N vertices  followed by list of triangles using 1...N indexing 
    public void loadTriangles( String filename ){
	try{

		
		    FileInputStream fis = new FileInputStream(filename); 
		    BufferedReader br = new BufferedReader( new InputStreamReader( fis ) );
		    String line = br.readLine(); while(line.startsWith("#"))line = br.readLine();
		    int nVertices = Integer.parseInt( line );
		    points = new Point3D[nVertices];

			for( int i=0;i<nVertices;i++){
				line = br.readLine(); while(line.startsWith("#"))line = br.readLine();
				StringTokenizer strtok = new StringTokenizer( line, " \t" );
				double x = Double.parseDouble( strtok.nextToken() );
				double y = Double.parseDouble( strtok.nextToken() );
				double z = Double.parseDouble( strtok.nextToken() );
				points[i] = new Point3D( x, y, z);
			}
		    line = br.readLine();  while(line.startsWith("#"))line = br.readLine();
		    int nTriangles = Integer.parseInt( line );
		    triangles = new Triangle[nTriangles];
		    for(int j=0; j < 1; j++){
		    for( int i=0;i<nTriangles;i++){
				line = br.readLine(); while(line.startsWith("#"))line = br.readLine();
				StringTokenizer strtok = new StringTokenizer( line, " \t" );
				int i1 = Integer.parseInt( strtok.nextToken() )-1;
				int i2 = Integer.parseInt( strtok.nextToken() )-1;
				int i3 = Integer.parseInt( strtok.nextToken() )-1;
				triangles[i] = new Triangle(i1, i2, i3);		
		    }
		    }
		    br.close();
		    fis.close();

		
	}catch(Exception e ){e.printStackTrace(); }	
    
    }

    public class DisplayArea extends JPanel{
	public DisplayArea( Rectangle bounds ){
	    setLayout(null);
	    setBounds( bounds);
	    setOpaque(false);
	    setPreferredSize( new Dimension (bounds.width, bounds.height )  );
	}

	public void paintComponent( Graphics g ){
	    Graphics2D g2 = (Graphics2D)g;

	    g2.setStroke( new BasicStroke( 0.5F ) );  // set thickness of rendered lines

	    // switch anti-aliasing ON  (warning slows code down a lot!)
	    if( antiAliasing )g2.addRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING,
						    RenderingHints.VALUE_ANTIALIAS_ON )  );

	    g2.setColor( Color.lightGray );
	    g2.fillRect( 0, 0, getWidth(), getHeight() );
	    g2.setColor( Color.black );
	    g2.translate( getWidth()/2, getHeight()/2 );   // make 0,0 the screen centre
	    g2.scale( 1.0, -1.0 );  // make the  x,y the normal right-handed Cartesian way

	    if( drawAxes ){
		Point3D origin = new Point3D( 0, 0, 0 );
		Point3D xAxis = new Point3D( 1, 0, 0 );
		Point3D yAxis = new Point3D( 0, 1, 0 );
		Point3D zAxis = new Point3D( 0, 0, 1 );

		g2.setColor( Color.red );
		draw3DLine( g2, origin, xAxis,3 );
		g2.setColor( Color.green );
		draw3DLine( g2, origin, yAxis,3 );
		g2.setColor( Color.blue );
		draw3DLine( g2, origin, zAxis,3 );
	    }

	    if( drawSurfaces )fillSomeTriangles(g2); 
	    if( drawWires )drawTriangles(g2); 
	    if( drawNormals )normedTriangles(g2); 
	}
    }

    public void draw3DLine( Graphics2D g2, Point3D start, Point3D finish, Integer rand ){
	Point2D s =  start.projectPoint();
	Point2D f = finish.projectPoint();
	if(rand.equals(0)){
		g2.drawLine( s.x, s.y, f.x, f.y);
	}else if(rand.equals(1)){
		g2.drawLine( s.x+ 250, s.y+ 250, f.x+ 250, f.y + 250);
	}else if(rand.equals(2)){
		g2.drawLine( s.x- 250, s.y+ 250, f.x- 250, f.y + 250);
	}else{
		g2.drawLine( s.x, s.y, f.x, f.y);
	}
    }

    public class Point2D{  // a simple  2D  point for rendering onto the display screen
	public int x;
	public int y;
	Point2D(){ x=0; y=0; }
	Point2D( int x, int y ){
	    this.x= x;
	    this.y= y;
	}
    }

    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    public int xScreenOrigin = 0;
    public int yScreenOrigin = 0;

    public Point3D screenPosition = new Point3D( 0, 0, 50 );
    public Point3D viewAngle = new Point3D( 0, 0, 180 );

    private double cosTheta = Math.cos( DEGREES_TO_RADIANS * viewAngle.x );
    private double sinTheta = Math.sin( DEGREES_TO_RADIANS * viewAngle.x );
    private double cosPhi   = Math.cos( DEGREES_TO_RADIANS * viewAngle.y );
    private double sinPhi   = Math.sin( DEGREES_TO_RADIANS * viewAngle.y );
    
    private double modelScale = 10.0;

    public void updateAngles(){
	cosTheta = Math.cos( DEGREES_TO_RADIANS * viewAngle.x );
	sinTheta = Math.sin( DEGREES_TO_RADIANS * viewAngle.x );
	cosPhi   = Math.cos( DEGREES_TO_RADIANS * viewAngle.y );
	sinPhi   = Math.sin( DEGREES_TO_RADIANS * viewAngle.y );
    }

    public void setDefaultView(){
	xScreenOrigin = 0;
	yScreenOrigin = 0;
	screenPosition = new Point3D( 0, 0, 20 );
	viewAngle = new Point3D( 0, 0, 180 );
	modelScale = 10.0;
    }

    public class Point3D{
	public double x;
	public double y;
	public double z;
	Point3D(){ x=0.0; y=0.0; z=0.0; }
	Point3D( double x, double y, double z ){ this.x= x; this.y= y; this.z= z; }
	Point3D( int x, int y, int z ){ this.x= x; this.y= y; this.z= z; }

	public Point2D projectPoint(){
	    Point2D retval = new Point2D();
	    double tmpx = screenPosition.x + x * cosTheta - y * sinTheta;
	    double tmpy = screenPosition.y + x * sinTheta + y * cosTheta * sinPhi
		                           + z * cosPhi ;
	    double temp = viewAngle.z / (screenPosition.z
					 + x * sinTheta * cosPhi
					 + y * cosTheta * cosPhi - z * sinPhi  );
	    retval.x = xScreenOrigin + (int)(modelScale * temp * tmpx );
	    retval.y = yScreenOrigin + (int)(modelScale * temp * tmpy );
	    return retval;
	}

	public String toString(){ return "[" + x + "," + y + "," + z + "]"; }

	public double magnitude(){
	    return Math.sqrt( x * x + y * y + z * z );
	}

	public double dotProduct( Point3D b ){
	    return x * b.x + y * b.y + z * b.z;
	}

	public Point3D crossProduct( Point3D b ){
	    Point3D c = new Point3D();
	    c.x = y * b.z - z * b.y;
	    c.y = z * b.x - x * b.z;
	    c.z = x * b.y - y * b.x;
	    return c;
	}

	public Point3D scale( double s ){
	    Point3D c = new Point3D();
	    c.x = s * x;
	    c.y = s * y;
	    c.z = s * z;
	    return c;
	}

	public Point3D add( Point3D b){
	    Point3D c = new Point3D();
	    c.x = x + b.x;
	    c.y = y + b.y;
	    c.z = z + b.z;
	    return c;
	}

	public Point3D minus( Point3D b){
	    Point3D c = new Point3D();
	    c.x = x - b.x;
	    c.y = y - b.y;
	    c.z = z - b.z;
	    return c;
	}
    }

    private JSlider xPosition    = null;
    private JSlider yPosition    = null;
    private JSlider zPosition    = null;
    private JSlider thetaView    = null;
    private JSlider phiView      = null;
    private JSlider zAngleView   = null;

    private JCheckBox doRotateX   = null;
    private JCheckBox doRotateY   = null;
    private JCheckBox doRotateZ   = null;

    public class ControlPanel extends JPanel implements ActionListener, ChangeListener{
	public ControlPanel( Rectangle bounds ){
	    setLayout(new FlowLayout() );
	    setBounds(bounds);
       	    setOpaque(false);
	    setPreferredSize( new Dimension (bounds.width, bounds.height )  );
	    setBackground( Color.gray );

	    JPanel sliders = new JPanel( new GridLayout( 6, 2 ) );
	    sliders.setBackground( Color.cyan );

	    zPosition = new JSlider( 0, 50, 30 );  // min, max, value
	    zPosition.addChangeListener( this );
	    zPosition.setMajorTickSpacing( 10 );
	    zPosition.setPaintTicks( true );
	    zPosition.setPaintLabels( true );
	    sliders.add( zPosition);
	    sliders.add( new JLabel( "zPos " )  );

	    xPosition = new JSlider( -5, 5, 0 );  // min, max, value
	    xPosition.addChangeListener( this );
	    xPosition.setMajorTickSpacing( 1 );
	    xPosition.setPaintTicks( true );
	    xPosition.setPaintLabels( true );
	    sliders.add( xPosition);
	    sliders.add( new JLabel( "xPos " )  );

	    yPosition = new JSlider( -5, 5, 0 );  // min, max, value
	    yPosition.addChangeListener( this );
	    yPosition.setMajorTickSpacing( 1 );
	    yPosition.setPaintTicks( true );
	    yPosition.setPaintLabels( true );
	    sliders.add( yPosition);
	    sliders.add( new JLabel( "yPos " )  );

	    thetaView = new JSlider( 0, 360, 0 );
	    thetaView.addChangeListener( this );
	    thetaView.setMajorTickSpacing( 90 );
	    thetaView.setPaintTicks( true );
	    thetaView.setPaintLabels( true );
	    sliders.add( thetaView);
	    sliders.add( new JLabel( "Theta" )  );

	    phiView   = new JSlider( 0, 360, 0 );
	    phiView.addChangeListener( this );
	    phiView.setMajorTickSpacing( 90 );
	    phiView.setPaintTicks( true );
	    phiView.setPaintLabels( true );
	    sliders.add( phiView);
	    sliders.add( new JLabel( "Phi  " )  );

	    zAngleView   = new JSlider( -360, 360, 180);
	    zAngleView.addChangeListener( this );
	    zAngleView.setMajorTickSpacing( 180 );
	    zAngleView.setPaintTicks( true );
	    zAngleView.setPaintLabels( true );
	    sliders.add( zAngleView);
	    sliders.add( new JLabel( "zAngle" )  );
	    add( sliders);

	    doRotateX = new JCheckBox("Rotate X?");
	    doRotateX.setBackground(Color.yellow);
	    doRotateX.addActionListener( this );
	    add( doRotateX );

	    doRotateY = new JCheckBox("Rotate Y?");
	    doRotateY.setBackground(Color.yellow);
	    doRotateY.addActionListener( this );
	    add( doRotateY );

	    doRotateZ = new JCheckBox("Rotate Z?");
	    doRotateZ.setBackground(Color.yellow);
	    doRotateZ.addActionListener( this );
	    add( doRotateZ );
	}

	public void paintComponent( Graphics g ){
	    Graphics2D g2 = (Graphics2D)g;
	    g2.setColor( Color.gray );
	    g2.fillRect( 0, 0, getWidth(), getHeight() );
	}
	
	public void actionPerformed( ActionEvent ev ){
	    if( ev.getSource() == doRotateX ){
                if( doRotateX.isSelected() )
		    rotatingX = true;
		else
		    rotatingX = false;
	    }
	    else if( ev.getSource() == doRotateY ){
                if( doRotateY.isSelected() )
		    rotatingY = true;
		else
		    rotatingY = false;
	    }
	    else if( ev.getSource() == doRotateZ ){
                if( doRotateZ.isSelected() )
		    rotatingZ = true;
		else
		    rotatingZ = false;
	    }
	}

	public void stateChanged( ChangeEvent cev ){
	    if( cev.getSource() == xPosition ){
		screenPosition = new Point3D( xPosition.getValue(),
					      screenPosition.y, screenPosition.z );
		da.repaint();
	    }
	    if( cev.getSource() == yPosition ){
		screenPosition = new Point3D( screenPosition.x, yPosition.getValue(),
					      screenPosition.z );
		da.repaint();
	    }
	    else if( cev.getSource() == zPosition ){
		screenPosition = new Point3D( screenPosition.x, screenPosition.y,
					      zPosition.getValue() );
		da.repaint();
	    }
	    else if( cev.getSource() == thetaView ){
		viewAngle = new Point3D( thetaView.getValue(),viewAngle.y,viewAngle.z );
		updateAngles();
		da.repaint();
	    }
	    else if( cev.getSource() == phiView ){
		viewAngle = new Point3D( viewAngle.x, phiView.getValue(), viewAngle.z );
		updateAngles();
		da.repaint();
	    }
	    else if( cev.getSource() == zAngleView ){
		viewAngle = new Point3D( viewAngle.x, viewAngle.y, zAngleView.getValue() );
		updateAngles();
		da.repaint();
	    }
	}
    }

    //---------------------------------------------------------------------------------------

    public class Point4D{  // a 4D  vector/point used for homogeneous  coords  in 3D
	public double x;
	public double y;
	public double z;
	public double w;
	Point4D(){ x=0.0; y=0.0; z=0.0; w=1.0; }
	Point4D( double x, double y, double z, double w ){ this.x=x; this.y=y; this.z=z; this.w=w;}
	Point4D( int x, int y, int z, int w ){ this.x=x; this.y=y; this.z=z; this.w=w; }
	Point4D( Point3D p ){ this.x=p.x; this.y=p.y; this.z=p.z; this.w = 1.0; }
	public void normalise(){ x/= w; y/=w; z/=w; w = 1.0; }
    }

    public void rotateX(){  // rotate the model about the X axis  in model world coordinates
	double[][] R = rotationX( 0.01 );
	for( int i = 0;i< points.length;i++){
	    Point4D p = new Point4D( points[i] );
	    p = multiply( R, p );
	    points[i].x = p.x;
	    points[i].y = p.y;
	    points[i].z = p.z;
	}

    }

    public void rotateY(){  // rotate the model about the Y axis  in model world coordinates
	double[][] R = rotationY( 0.01 );
	for( int i = 0;i< points.length;i++){
	    Point4D p = new Point4D( points[i] );
	    p = multiply( R, p );
	    points[i].x = p.x;
	    points[i].y = p.y;
	    points[i].z = p.z;
	}

    }

    public void rotateZ(){  // rotate the model about the Z axis  in model world coordinates
	double[][] R = rotationZ( 0.01 );
	for( int i = 0;i< points.length;i++){
	    Point4D p = new Point4D( points[i] );
	    p = multiply( R, p );
	    points[i].x = p.x;
	    points[i].y = p.y;
	    points[i].z = p.z;
	}

    }

    public double[][] multiply( double[][] B, double[][] C ){  // return A = B C
	double[][] A = new double[4][4];
	for(int i=0;i<4;i++){
	    for(int j=0;j<4;j++){
		A[i][j] = 0.0;
		for(int k=0;k<4;k++){
		    A[i][j] += B[i][k] * C[k][j];
		}
	    }
	}
	return A;
    }

    public Point4D multiply( double[][] M, Point4D p ){  // return q = Mp
	Point4D q = new Point4D();
	q.x = M[0][0] * p.x  +  M[0][1] * p.y  +  M[0][2] * p.z  +  M[0][3] * p.w;
	q.y = M[1][0] * p.x  +  M[1][1] * p.y  +  M[1][2] * p.z  +  M[1][3] * p.w;
	q.z = M[2][0] * p.x  +  M[2][1] * p.y  +  M[2][2] * p.z  +  M[2][3] * p.w;
	q.w = M[3][0] * p.x  +  M[3][1] * p.y  +  M[3][2] * p.z  +  M[3][3] * p.w;
	return q;
    }

    public double[][] rotationX( double theta ){  // rotation Matrix about X axis
	double[][] r = new double[4][4];
	r[0][0] = 1.0;  r[0][1] =  0.0;              r[0][2] =  0.0;              r[0][3] = 0.0;
	r[1][0] = 0.0;  r[1][1] =  Math.cos(theta);  r[1][2] = -Math.sin(theta);  r[1][3] = 0.0;
	r[2][0] = 0.0;  r[2][1] =  Math.sin(theta);  r[2][2] =  Math.cos(theta);  r[2][3] = 0.0;
	r[3][0] = 0.0;  r[3][1] =  0.0;              r[3][2] =  0.0;              r[3][3] = 1.0;
	return r;
    }

    public double[][] rotationY( double theta ){  // rotation Matrix about Y axis
	double[][] r = new double[4][4];
	r[0][0] =  Math.cos(theta);  r[0][1] =  0.0;  r[0][2] = Math.sin(theta);  r[0][3] = 0.0;
	r[1][0] =  0.0;              r[1][1] =  1.0;  r[1][2] = 0.0;              r[1][3] = 0.0;
	r[2][0] = -Math.sin(theta);  r[2][1] =  0.0;  r[2][2] = Math.cos(theta);  r[2][3] = 0.0;
	r[3][0] =  0.0;              r[3][1] =  0.0;  r[3][2] = 0.0;              r[3][3] = 1.0;
	return r;
    }

    public double[][] rotationZ( double theta ){  // rotation Matrix about Z axis
	double[][] r = new double[4][4];
	r[0][0] = Math.cos(theta); r[0][1] = -Math.sin(theta);  r[0][2] = 0.0;  r[0][3] = 0.0;
	r[1][0] = Math.sin(theta); r[1][1] =  Math.cos(theta);  r[1][2] = 0.0;  r[1][3] = 0.0;
	r[2][0] = 0.0;             r[2][1] =  0.0;              r[2][2] = 1.0;  r[2][3] = 0.0;
	r[3][0] = 0.0;             r[3][1] =  0.0;              r[3][2] = 0.0;  r[3][3] = 1.0;
	return r;
    }

    public double[][] translation( double dx, double dy, double dz ){  // translation Matrix
	double[][] r = new double[4][4];
	r[0][0] = 1.0;  r[0][1] = 0.0;  r[0][2] = 0.0;  r[0][3] =  dx;
	r[1][0] = 0.0;  r[1][1] = 1.0;  r[1][2] = 0.0;  r[1][3] =  dy;
	r[2][0] = 0.0;  r[2][1] = 0.0;  r[2][2] = 1.0;  r[2][3] =  dz;
	r[3][0] = 0.0;  r[3][1] = 0.0;  r[3][2] = 0.0;  r[3][3] = 1.0;
	return r;
    }

    public double[][] scaling( double sx, double sy, double sz ){  // scaling Matrix
	double[][] r = new double[4][4];
	r[0][0] =  sx;  r[0][1] = 0.0;  r[0][2] = 0.0;  r[0][3] = 0.0;
	r[1][0] = 0.0;  r[1][1] =  sy;  r[1][2] = 0.0;  r[1][3] = 0.0;
	r[2][0] = 0.0;  r[2][1] = 0.0;  r[2][2] =  sz;  r[2][3] = 0.0;
	r[3][0] = 0.0;  r[3][1] = 0.0;  r[3][2] = 0.0;  r[3][3] = 1.0;
	return r;
    }

    public double[][] identity(){  // Identity Matrix
	double[][] r = new double[4][4];
	r[0][0] = 1.0;  r[0][1] = 0.0;  r[0][2] = 0.0;  r[0][3] = 0.0;
	r[1][0] = 0.0;  r[1][1] = 1.0;  r[1][2] = 0.0;  r[1][3] = 0.0;
	r[2][0] = 0.0;  r[2][1] = 0.0;  r[2][2] = 1.0;  r[2][3] = 0.0;
	r[3][0] = 0.0;  r[3][1] = 0.0;  r[3][2] = 0.0;  r[3][3] = 1.0;
	return r;
    }

}
