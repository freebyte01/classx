package classx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class ClassX extends JFrame {

	public static class Point{
		double x,y,l=Double.MIN_VALUE;
		Point u, n;

		public Point(double x, double y) {
			this.x= x; 
			this.y= y;
		}
		Point set(double x, double y) {
			this.x= x; 
			this.y= y;
			return this; }
		public Point clone(){	return new Point(x,y); }
		public double size(){	return l==Double.MIN_VALUE ? l= Math.sqrt(x*x+y*y) : l; }
		public Point vect(Point p){ return new Point(p.x-x, p.y-y); }
		public Point vect(double xx, double yy){ return new Point(xx-x, yy-y); }
		static public Point vect(Point a, Point b){ return new Point(b.x-a.x, b.y-a.y); }
		static public Point vect(double ax, double ay, double bx, double by){ return new Point(bx-ax, by-ay); }
		public Point add(Point p){ return new Point(x+p.x, y+p.y); }
		public Point add(double xx, double yy){ return new Point(x+xx, y+yy); }
		public Point mul(double m){ return new Point(x*m, y*m); }
		public Point unit(){ return u==null?u= mul(1/size()):u; }
		public Point n(){ return n==null?n=new Point(-y, x):n; }
	}

	final DepViewer panel;
	public ClassX() {
		setContentPane(panel=new DepViewer());
		addFocusListener(panel);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(1200, 1000);
		setVisible(true);
	}

	class DepViewer extends JPanel implements MouseMotionListener, MouseListener, KeyListener, MouseWheelListener, FocusListener{

		private void drawLine(Graphics2D g, Point a, Point b){ g.draw( new Line2D.Double(a.x, a.y, b.x, b.y));	}
		private void drawLine(Graphics2D g, double ax, double ay, double bx, double by){ g.draw( new Line2D.Double(ax, ay, bx, by));	}
		private void fillOval(Graphics2D g, Point a, Point b){ g.fill( new Ellipse2D.Double( a.x, a.y, b.x-a.x, b.y-a.y));	}
		private void drawRoundRect(Graphics2D g, Point a, Point b, double c1, double c2){ g.draw( new RoundRectangle2D.Double( a.x, a.y, b.x-a.x, b.y-a.y, c1, c2));	}
		private void fillRoundRect(Graphics2D g, Point a, Point b, double c1, double c2){ g.fill( new RoundRectangle2D.Double( a.x, a.y, b.x-a.x, b.y-a.y, c1, c2));	}

		private final String trim= "E:\\apps\\myp\\GUI\\cockpit\\src\\";

		VClass over=null, selected=null;

		public DepViewer() {
			addMouseMotionListener(this);
			addMouseListener(this);
			addKeyListener(this);
			addMouseWheelListener(this);
			addFocusListener(this);

			KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(new KeyEventDispatcher() {
				public boolean dispatchKeyEvent(KeyEvent e) {
					if (KeyEvent.KEY_PRESSED == e.getID())
						keyPressed(e);
					if (KeyEvent.KEY_RELEASED == e.getID())
						keyReleased(e);
					if (KeyEvent.KEY_TYPED == e.getID())
						keyTyped(e);
					return false; }}); }

		public double r=getWidth()/4, c=2, zoom=1, sx, sy;
		double sa=0, a=sa, s=2*Math.PI/clss.size();

		protected boolean checkClassPos(){

			r=getWidth()/4;
			a=sa; 
			s=2*Math.PI/clss.size();

			VClass lOver= over;
			over=mP?over:null;

			// current ring positions
			synchronized (clss) {

				for (VClass cls : clss.values()){
					if (ctrl || (alt?cls.apos==null:cls.pos==null)){
						double cs= Math.cos(a);
						double sn= Math.sin(a);
						cls.p.set(cs*r, sn*r); 
					} else 
						cls.p.set((alt?cls.apos.x:cls.pos.x), (alt?cls.apos.y:cls.pos.y));
					if (!mP && (cls.isBus()?rX>cls.minX-10 && rX<cls.maxX+10 && Math.abs(rY-cls.p.y)<10 || 	cls.selTL!=null && rX>cls.selTL.x && rX<cls.selBR.x && rY>cls.selTL.y && rY<cls.selBR.y :new Point(rX-cls.p.x, rY-cls.p.y).size()<10) ) 
						over= cls;
					a+=s; }}
			return over!=lOver; }


		BasicStroke bus, thick, sNormal, thin, link;
		double rX, rY;

		protected void paintComponent(Graphics g) {
			// TODO Auto-generated method stub
			super.paintComponent(g);
			Graphics2D g2= (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,  RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,  RenderingHints.VALUE_COLOR_RENDER_QUALITY);

			double zm= ctrl?1:zoom;
			g2.translate(getWidth()/2, getHeight()/2);
			if (!ctrl) {
				g2.scale(zm, zm);
				g2.translate(sx, sy); }

			g2.setColor(Color.blue);
			//g2.fill(new Ellipse2D.Double(rX-2, rY-2, 4, 4));

			bus= new BasicStroke((float) (2));
			thick= new BasicStroke((float) (1.5));
			sNormal= new BasicStroke((float) (1));
			thin= new BasicStroke((float) (.5));
			link= new BasicStroke((float) (3), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0);
			Font bold= new Font("Arial", Font.BOLD, (int) Math.round(12));
			Font normal= new Font("Arial", Font.PLAIN, (int) Math.round(12));
			Font small= new Font("Arial", Font.PLAIN, (int) Math.round(10));
			double timeFactor= (System.currentTimeMillis()-lastOverTime)/1000d;
			timeFactor= (over==null?1:1-(timeFactor>1?1:timeFactor));
			Color cGhost= new Color(200,200,200,(int) Math.floor(255*timeFactor));
			Color cBus= Color.gray;
			Color cMark= new Color(50,50,255);
			double connSize=8;


			synchronized (clss) {

				//g2.draw(new Ellipse2D.Double(mX-1, mY-1, 2, 2));
				checkClassPos();
				
				for (VClass cls : clss.values()){
					cls.minX= cls.maxX= cls.overX= cls.p.x;
					cls.mark= over!=null && (over==cls || cls.deps.contains(over.fId) || cls.refs.contains(over.fId) || cls.sels.contains(over.fId) ||  over.deps.contains(cls.fId) || over.refs.contains(cls.fId) || over.sels.contains(cls.fId));
					cls.cMain= cls.mark ? cMark : over==null? selected==cls? Color.black : cBus : cGhost;
					cls.cSec= over==cls? cls.cMain : over==null? selected==cls? Color.black : cBus:cGhost;
				}				
				
				// dependencies
				for (VClass cls : clss.values()){
					int depCnt=1;
					if (!alt)
						for (String dep : cls.deps){
							VClass depC= clss.get(dep);
							if (depC!=null){
								if (!depC.refs.contains(cls)) depC.refs.add(cls);
								//								double fx= cls.isBus()? depC.isBus()? (cls.p.x+ depC.p.x)/2 : depC.p.x: cls.p.x;
								double fx= cls.p.x+ 0*(!ctrl && cls.isBus()?depCnt++:0);
								if (fx<cls.minX) cls.minX= fx;
								if (fx>cls.maxX) cls.maxX= fx;
								Point f= new Point(fx, cls.p.y);

								//								double tx= depC.isBus()? cls.isBus()? (cls.p.x+ depC.p.x)/2 : cls.p.x : depC.p.x;
								double tx= !ctrl && depC.isBus()? fx : depC.p.x;
								if (tx<depC.minX) depC.minX= tx;
								if (tx>depC.maxX) depC.maxX= tx;

								Point t= new Point( tx, depC.p.y);

								Point v= f.vect(t);
								g2.setStroke(thin);
								g2.setColor(cls==over?depC.cMain:depC.cSec);
								
								//g2.setColor(cls==over || depC==over?Color.blue:cls==selected || depC==selected?Color.black: over!=null?cGhost:Color.lightGray);
								drawLine( g2, f.add(v.unit()), f.add(v.unit().mul(v.size()/2)));

								g2.setStroke(thick);

								drawLine( g2, f.add(v.unit().mul(v.size()/2)), f.add(v));

								
								if (!ctrl){
									if (false && cls.isBus()){
										g2.setColor(Color.black);
										drawLine( g2, f.add(v.unit().n().mul(-connSize)), f.add(v.unit().mul(connSize)));
										drawLine( g2, f.add(v.unit().n().mul(connSize)), f.add(v.unit().mul(connSize)));
										drawLine( g2, f.add(v.unit().n().mul(-connSize)), f.add(v.unit().n().mul(connSize))); }
									if (depC.isBus()){
										if (cls==over) depC.overX= t.x; 
										GeneralPath tr = new GeneralPath();
										Point p;
										p= t.add(v.unit().n().mul(-connSize)); tr.moveTo( p.x, p.y );
										p= t.add(v.unit().mul(-connSize)); tr.lineTo( p.x, p.y );
										p= t.add(v.unit().n().mul(connSize)); tr.lineTo( p.x, p.y );
										g2.fill(tr); }}
							}}
					
					// templates dependencies
						for (String sel : cls.sels){
							VClass depC= clss.get(sel);
							if (depC!=null){

								double fx= depC.isBus()? cls.isBus()? (cls.p.x+ depC.p.x)/2 : cls.p.x : depC.p.x;
								if (fx<depC.minX) depC.minX= fx;
								if (fx>depC.maxX) depC.maxX= fx;
								Point f= new Point(fx, depC.p.y);

								double tx= cls.isBus()? depC.isBus()? (cls.p.x+ depC.p.x)/2 : depC.p.x: cls.p.x;
								if (tx<cls.minX) cls.minX= tx;
								if (tx>cls.maxX) cls.maxX= tx;

								Point v= Point.vect(fx, depC.p.y, tx, cls.p.y);

								g2.setStroke(thin);

								g2.setColor(!alt?new Color(200,200,255):cls.mark?cMark:cGhost);
								drawLine( g2, f.add(v.unit().mul(20)), f.add(v.unit().mul(v.size()/2)));
								fillOval(  g2,  f.add(v.unit().mul(20)).add(-2,-2), f.add(v.unit().mul(20)).add(2,2) );
								
								g2.setStroke(thick);
								//g2.setColor(!alt?new Color(240,240,255):cls==over || depC==over?Color.blue:cls==selected || depC==selected?Color.black:Color.lightGray);
								drawLine( g2, f.add(v.unit().mul(v.size()/2	)), f.add(v)); }}}


				// labels
				g2.setColor(Color.black);
				AffineTransform at= (AffineTransform) g2.getTransform().clone();
				a=sa;
				for (VClass cls : clss.values())
					if (alt && cls.sel==null){
						a+=s;
						continue;
					} else {
						boolean flipY= ctrl || (alt?cls.apos:cls.pos)==null?(a+Math.PI/2)%(Math.PI*2)>Math.PI:false;
						if (flipY) g2.scale(-1,-1);
						g2.translate(flipY?-cls.p.x:cls.p.x, flipY?-cls.p.y:cls.p.y);
						c= 5;

						g2.setColor(cls.cMain);

						double ww= 0; // some additional widths
						
						
						if (ctrl || (alt?cls.apos:cls.pos)==null) g2.rotate(a);
						g2.setFont(bold);

						g2.fill( new Ellipse2D.Double( -c, -c, c*2, c*2));
						if (selected==cls){
							c= 16;
							g2.setStroke(thick);
							g2.draw( new Ellipse2D.Double( -c, -c, c*2, c*2)); }
						
						g2.setColor(cls.cSec);
						// bus line
						if (!ctrl && !alt && cls.isBus()){
							g2.setStroke(new BasicStroke((float)(ww=Math.sqrt(cls.refs.size()/2f)+0.5)));
							drawLine(g2, cls.minX-cls.p.x-7, 0, cls.maxX-cls.p.x+7, 0);
							if (cls.overX!=cls.p.x)
								g2.setColor(cls.cMain);
								drawLine(g2, cls.overX-cls.p.x+(cls.p.x>cls.overX?-connSize+1:connSize-1), 0, 0, 0); }

						

						
						g2.setColor(cls.cMain);
						double sw= g2.getFontMetrics().stringWidth(cls.id);

						double offX=0;
//						g2.setFont(small);
						if (cls==over) System.err.println((cls.p.x+sx)*zm+getWidth()/2+ ":"+ getWidth());
						if (cls==over && !flipY && (cls.p.x+sx)*zm+getWidth()/2>getWidth()-sw*zm)
							offX= rX-cls.p.x;
//							g2.drawString( cls.id, Math.round(c-100),  10 );

						if (cls.isBus()){
//							g2.setStroke(thin);
							cls.selTL= new Point(Math.round(flipY?-sw-c-4:c+4+offX),  Math.round(-8-ww/2));
							cls.selBR= cls.selTL.add(sw+8+ww*2, 16+ww);
							g2.setColor(over==cls?new Color(255,255,0):selected==cls?new Color(250,250,250):new Color(230,230,230));					
							fillRoundRect(g2, cls.selTL, cls.selBR, 12, 12);
							g2.fill(new RoundRectangle2D.Double(Math.round(flipY?-sw-c-4:c+4+offX),  Math.round(-8-ww/2), sw+8+ww*2, 16+ww, 12, 12)); 
							g2.setColor(over==cls || over==null?cBus:cGhost);
							drawRoundRect(g2, cls.selTL, cls.selBR, 12, 12);
							cls.selTL= cls.selTL.add(cls.p);
							cls.selBR= cls.selBR.add(cls.p);
						} else
							g2.setStroke(thin);
						
						g2.setColor(cls.cMain);
						
						
						
						if (cls.id.toLowerCase().contains("subscription")) paintConnectionIcon(g2, 0, 0);
						else if (cls.id.toLowerCase().contains("service")) paintServiceIcon(g2, 0, 0);
						else if (cls.sel!=null) paintTemplateIcon(g2, 0, 0, cls);



						//						
//						// label background
//						if (cls.deps.size()==0){
//							g2.setColor(new Color(30,30,30,20));
//							g2.fill(new RoundRectangle2D.Double(Math.round(flipY?-sw-c-4:c+4)-4,  Math.round(-8), sw+8, 16, 4, 6)); }
//						if(cls.refs.size()==0){
//							g2.setColor(new Color(250,250,0,30));
//							g2.fill(new RoundRectangle2D.Double(Math.round(flipY?-sw-c-4:c+4)-4,  Math.round(-9), sw+8, 18, 4, 6)); }
						
						// label
						g2.drawString( cls.id, Math.round(flipY?-sw-c-2:c+8+offX+ww),  Math.round(3.5) );


						
						// src file
						g2.setFont(normal);
						String srcT= cls.src.replace(trim, "");
						if (cls==over || (ctrl || shift || (alt?cls.apos:cls.pos)==null))
							if (ctrl || (alt?cls.apos:cls.pos)==null)
								g2.drawString( srcT, Math.round(flipY?-sw-c-4-g.getFontMetrics().stringWidth(srcT)-10:c+4+sw+10),  cls.isBus()?0:Math.round(4.5));
							else {
								g2.setFont(small);
								g2.drawString( srcT, Math.round(c+10),  Math.round(16+ww)); }
						
						

						// is selector -> template
						if (cls.sel!=null) {
							g2.setFont(small);
							if (ctrl|| (alt?cls.apos:cls.pos)==null)
								g2.drawString( cls.sel, Math.round(!flipY?-c-4-g.getFontMetrics().stringWidth(cls.sel)-10:c+8+10),  Math.round(4.5));
							else 
								g2.drawString( cls.sel, Math.round(c+6),  Math.round(-8)); }

						double i=1.5;
						for (String dep : cls.deps){
							VClass depC= clss.get(dep);
							if (depC==null)
								//if (ctrl || cls.pos==null)
								g2.drawString( dep, Math.round(flipY?-c-4-sw-100-g.getFontMetrics().stringWidth(dep):c+100), Math.round(i++*12) );}

						g2.setTransform((AffineTransform)at.clone());
						g2.setFont(small);
						a+=s;
					}}}

		public void paintTemplateIcon(Graphics2D g, double x, double y, VClass cls){
			AffineTransform ot=g.getTransform(); Color oc= g.getColor(); Stroke os= g.getStroke();
			g.translate(x, y); 
			g.setColor(over==cls || over!=null && cls.sels.contains(over.fId)? Color.yellow : selected==cls || selected!=null && cls.sels.contains(selected.fId) ? Color.blue : Color.white);
			g.fill(new RoundRectangle2D.Double(-8,-8, 16, 16, 12, 12));
			g.setColor(cls.cMain);
			g.setStroke(bus);
			g.draw(new RoundRectangle2D.Double(-8,-8, 16, 16, 12, 12));
			g.setColor(oc); g.setStroke(os); g.setTransform(ot); }

		public void paintConnectionIcon(Graphics2D g, double x, double y){
			AffineTransform ot=g.getTransform(); Color oc= g.getColor(); Stroke os= g.getStroke();
			g.translate(x, y); 
			g.setColor(Color.white);
			g.fill(new RoundRectangle2D.Double(-8,-6, 16, 12, 8, 8));
			g.setColor(Color.red);
			GeneralPath gp= new GeneralPath();
			gp.moveTo(-1, -2); gp.lineTo(-1, -5); gp.lineTo(-7, 0); gp.lineTo(-1, 5); gp.lineTo(-1, 2);
			gp.lineTo(1, 2); gp.lineTo(1, 5); gp.lineTo(7, 0); gp.lineTo(1, -5); gp.lineTo(1, -2);
			g.fill(gp); 
			g.setColor(oc); g.setStroke(os); g.setTransform(ot); }

		
		public void paintServiceIcon(Graphics2D g, double x, double y){
			AffineTransform ot=g.getTransform(); Color oc= g.getColor(); Stroke os= g.getStroke();
			g.translate(x, y);
			g.setColor(Color.white);
			g.fill(new Ellipse2D.Double(-6, -6, 12, 12));
			g.setColor(Color.blue);	g.setStroke(thin);
			g.draw(new Ellipse2D.Double(-6, -6, 12, 12));
			g.fill(new Ellipse2D.Double(-2, -2, 4, 4)); 
			g.setColor(oc); g.setStroke(os); g.setTransform(ot); }


		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount()==2){
				if (over!=null && over.srcF!=null){
				    try{
				    	System.err.println(over.srcF.getCanonicalPath());
				    	Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "runEditor.bat", over.srcF.getCanonicalPath()});; }catch(Exception ee){ ee.printStackTrace();}
				}
				
			}
		}
		public void mouseEntered(MouseEvent arg0) {}
		public void mouseExited(MouseEvent arg0) {}

		long lastOverTime=0;
		public void mouseMoved(MouseEvent e) {
			ctrl=e.isControlDown();
			shift= e.isShiftDown();
			alt= e.isAltDown();
			mX=e.getX();
			mY=e.getY();
			rXo= rX;
			rYo= rY;
			rX=(mX-getWidth()/2)/zoom -sx;
			rY=(mY-getHeight()/2)/zoom -sy;
			if (!drag && checkClassPos()){
				lastOverTime= System.currentTimeMillis();
				repaint(); }}

		double mX, mY, rXo, rYo, vXo, vYo, dX, dY;
		boolean mP=false;
		public void mousePressed(MouseEvent e) {
			ctrl=e.isControlDown();
			shift= e.isShiftDown();
			alt= e.isAltDown();
			mouseMoved(e);
			vXo= (mX=e.getX())-getWidth()/2;
			vYo= (mY=e.getY())-getHeight()/2;
			if (over!=null) 
				selected= over;
			mP= true; }
		public void mouseReleased(MouseEvent arg0) {
			if (!drag) 
				selected= over;
			drag=false;
			mP= false;
			if (!alt) savePositions(positions, ".positions");
			else savePositions(apositions, ".apositions");
			saveDefs(".defs");
			repaint(); }

		boolean drag;
		public void mouseDragged(MouseEvent e) {
			drag= true;

			mouseMoved(e);
			
			if (ctrl){
				double vX= mX-getWidth()/2;
				double vY= mY-getHeight()/2;
				double nX= -vY;
				double nY= vX;
				double cos= (vXo*vX+ vYo*vY)/Math.sqrt(vXo*vXo+vYo*vYo)/Math.sqrt(vX*vX+vY*vY);
				double sin= (vXo*nX+ vYo*nY)/Math.sqrt(vXo*vXo+vYo*vYo)/Math.sqrt(vX*vX+vY*vY);
				sa+= Math.acos(cos)*(sin>0?-1:1); 
				vXo= vX;
				vYo= vY; 
			} else
				if (selected!=null && selected==over){
					if (!alt){ 
						if (selected.pos==null) selected.pos= new Point(rX, rY);
						else selected.pos= selected.pos.add(Math.abs(selected.pos.x-rXo)<10?rX-rXo:0, rY-rYo);
						positions.put(selected.fId, selected.pos); 
					} else {
						System.err.println(">");
						if (selected.apos==null) selected.apos= new Point(rX, rY);
						else selected.apos.add((selected.pos.x-rXo)<10?rX-rXo:0, rY-rYo);
						apositions.put(selected.fId, selected.apos);
					}
				} else {
					sx+= (rX-rXo);
					sy+= (rY-rYo);
					mouseMoved(e);
				}
			repaint();	}


		boolean ctrl, shift, alt;
		public void keyPressed(KeyEvent e) {
			ctrl=e.isControlDown();
			shift= e.isShiftDown();
			alt= e.isAltDown();
			checkClassPos(); 
			repaint(); }

		public void keyReleased(KeyEvent e) {
			ctrl=e.isControlDown();
			shift= e.isShiftDown();
			alt= e.isAltDown(); 
			checkClassPos();
			repaint(); }

		public void keyTyped(KeyEvent e) {}


		public void mouseWheelMoved(MouseWheelEvent e) {
			System.err.println(zoom);
			zoom-= e.getWheelRotation()*zoom/10d;
			sx=(mX-getWidth()/2)/zoom-rX;
			sy=(mY-getHeight()/2)/zoom-rY;
			repaint();
		}
		public void focusGained(FocusEvent arg0) { System.err.println("focused"); repaint(); }
		public void focusLost(FocusEvent arg0) {}

	}

	public static class VClass implements Comparable<VClass>{
		public final ArrayList<String> deps;
		public final ArrayList<String> sels= new ArrayList<String>();
		public final ArrayList<VClass> refs= new ArrayList<VClass>();
		final ArrayList<String> props= new ArrayList<String>();
		final File srcF;
		final File tempF;
		final String id, fId, src, sel, temp;
		Point p= new Point(0,0), pos, apos, selTL, selBR;
		boolean bus, vert, mark;
		Color cMain= Color.lightGray;
		Color cSec= Color.lightGray;
		double minX, maxX, overX;

		public VClass(String id, File srcF, String src, ArrayList<String> deps, String sel, String temp, File tempF) {
			this.id= id.intern();
			this.fId= (id+" @ "+src).intern();
			this.src= src;
			this.deps= deps;
			this.srcF= srcF;
			this.sel= sel;
			this.temp= temp;
			this.tempF= tempF;
			readFromDefs(defs.get(fId));
			Point p= positions.get(fId);
			if (p!=null) 
				pos=new Point(p.x, p.y);
			p= apositions.get(fId);
			if (p!=null) 
				apos=new Point(p.x, p.y);
		}
		public int compareTo(VClass o) {
			int c= id.compareTo(o.id);
			return c==0?src.compareTo(o.src):c; }

		boolean isBus(){
			return (refs.size())>2;
		}

		boolean isVert(){
			return false;
		}

		void readFromDefs(String def){
			if (def==null) return;
			String[] ll= def.split(";");
			if (ll.length<2) return;
			String x=null, y=null, ax=null, ay=null, bus="", vert="";
			for (String s : ll)
				if (s.trim().startsWith("x:")) x= s.split(":")[1];
				else if (s.trim().startsWith("y:")) y= s.split(":")[1];
				else if (s.trim().startsWith("bus:")) bus= s.split(":")[1];
				else if (s.trim().startsWith("vert:")) vert= s.split(":")[1];
			this.pos= x==null?null:new Point(Double.parseDouble(x),Double.parseDouble(y));
			this.apos= ax==null?null:new Point(Double.parseDouble(ax),Double.parseDouble(ay));
			this.bus= Boolean.parseBoolean(bus);
			this.vert= Boolean.parseBoolean(vert);}

		String saveToDefs(){
			return (pos==null?"":"x:"+pos.x+";y:"+pos.y)+(apos==null?"":";ax:"+apos.x+";ay:"+apos.y)+";bus:"+bus+";vert:"+vert;
		}
	}

	static String filterTS="*.ts"; 
	static String filterSel="*.ts,*.html"; 

	static LinkedHashMap<String, VClass> clss= new LinkedHashMap<String, VClass>();

	static void analyzeFile(File f){
		if (!f.exists() || f.getName().startsWith(".")) return;
		if (f.isDirectory()){
			for (File ff : f.listFiles())
				analyzeFile(ff);
		} else {
			int ix= f.getName().lastIndexOf(".");
			//			System.err.println(f.getName().substring(ix+1));
			if (ix<0 || !filterTS.contains(f.getName().substring(ix+1))) return;
			BufferedReader r= null;
			try{
				String fsrc= f.getCanonicalPath(), src, temp=null;
				src= fsrc.startsWith(WATCH_PATH)? fsrc.substring(WATCH_PATH.length()+1) : fsrc;
				System.err.println(": "+src);
				ArrayList<String> used=new ArrayList<String>();
				HashMap<String, String> imps= new HashMap<String, String>();
				r= new BufferedReader(new FileReader(f));
				String line, sel=null, currClass=null;
				File tempF=null;
				while ((line= r.readLine())!=null){
					line= line.replaceAll("[ ]+"," ");
					if (line.contains("selector:")){
						sel= line.split("selector:")[1].replace('\'',' ').replace(',',' ').replace('[',' ').replace(']',' ').trim();
					} else if (line.contains("template:")){
						temp="";
						boolean began= line.contains("`");
						while ((line= r.readLine())!=null)
							if (line.contains("`"))
								if ( began) break;
								else began= true;
							else
								temp+= line+"\n";
						System.err.println("temp:\n"+ temp);

					} else if (line.contains("templateUrl:")){
						System.err.println("tempUrl:"+ line.split("templateUrl:")[1].replace('\'',' ').replace(',',' ').trim());
						tempF= new File(f.getParentFile(), line.split("templateUrl:")[1].replace('\'',' ').replace(',',' ').trim());
					} else if (line.contains("export class") || line.contains("export default class")){
						if (currClass!=null){
							VClass n= new VClass(currClass, f, src, used, sel, temp, tempF);
							clss.put(n.fId, n);
							used= new ArrayList<String>();
							sel=null;
							temp=null;
							tempF=null; }
						String[] ss= line.split("class");
						currClass= ss[1].replace('{', ' ').replace('}', ' ').trim();
						System.err.println("class "+ currClass);
					} else if (line.contains("import") && line.contains("from") && !line.contains("@angular")){
						String[] ss= (line.indexOf('{')<0 && line.indexOf('}')<0? line.replace("import", "import { ").replace("from", " } from"):line).split("\\{|\\}");
						if (ss.length<3) continue;

						String imp, fimp= ss[2].replace("from","").replace('\'', ' ').replace('"', ' ').replace(';', ' ').trim()+".ts";
						File ff= new File(f.getParentFile(), fimp);
						fimp=ff.getCanonicalPath();
						imp= fimp.startsWith(WATCH_PATH) ? fimp.substring(WATCH_PATH.length()+1) : fimp;

						//if (ff.exists()) System.err.println("<<"+ff.length()+" : "+imp);

						String[] clss= ss[1].trim().split(",");
						for (String cls : clss){ 
							imps.put(cls.trim().intern(), cls=(cls.trim()+" @ "+imp).intern());
							//	System.err.println("+ "+cls);
						}
					} else 
						if (currClass!=null){
							for (String imp : imps.keySet())
								if (line.contains(imp) && !used.contains(imps.get(imp)))
									used.add(imps.get(imp)); 

						}
				}
				if (currClass!=null){
					VClass n= new VClass(currClass, f, src, used, sel, temp, tempF);
					clss.put(n.fId, n);
					used= new ArrayList<String>();
					sel=null;
					temp=null;
					tempF=null; }

			} catch (Throwable t){
			} finally {
				if (r!=null) try { r.close(); } catch (Exception e) { e.printStackTrace(); }
			}
		}

	}

	static boolean findSelPoint(File f, String sel){
		if (!f.exists() || f.getName().startsWith(".")) return false;
		if (f.isDirectory()){
			for (File ff : f.listFiles())
				findSelPoint(ff, sel);
		} else {
			int ix= f.getName().lastIndexOf(".");
			//			System.err.println(f.getName().substring(ix+1));
			if (ix<0 || !filterSel.contains(f.getName().substring(ix+1))) return false;
			BufferedReader r= null;
			try{
				String src= f.getCanonicalPath();
				ArrayList<String> deps= new ArrayList<String>();
				r= new BufferedReader(new FileReader(f));
				String line;
				StringBuffer sb= new StringBuffer();
				while ((line= r.readLine())!=null)
					sb.append(line+" ");//
				//line= line.replaceAll("[ ]+"," ");
				if (sb.toString().replaceAll("\"[^\"]*\"","").matches(sel)){// contains(sel)){
					return true; }

			} catch (Throwable t){
			} finally {
				if (r!=null) try { r.close(); } catch (Exception e) { e.printStackTrace(); }
			}
		}
		return false; }

	public static void savePositions(HashMap<String, Point> set, String fName){
		StringBuffer sb= new StringBuffer();
		synchronized (clss) {
			for (Entry<String, Point> e : set.entrySet())
				sb.append("x:"+e.getValue().x+";y:"+e.getValue().y+";id:"+e.getKey()+"\n"); }
		BufferedOutputStream bos=null;
		try{
			bos= new BufferedOutputStream(new FileOutputStream(new File(fName)));
			bos.write(sb.toString().getBytes());
		} catch(Throwable t){ t.printStackTrace();
		}finally{ if (bos!=null) try { bos.close(); } catch (Exception e) {} }}

	public static void saveDefs(String fName){
		StringBuffer sb= new StringBuffer();
		synchronized (clss) {
			for (VClass c : clss.values())
				sb.append("id:"+c.id+";"+c.saveToDefs()+"\n"); }
		BufferedOutputStream bos=null;
		try{
			bos= new BufferedOutputStream(new FileOutputStream(new File(fName)));
			bos.write(sb.toString().getBytes());
		} catch(Throwable t){ t.printStackTrace();
		}finally{ if (bos!=null) try { bos.close(); } catch (Exception e) {} }}

	protected static HashMap<String, Point> positions= new HashMap<String, ClassX.Point>();
	protected static HashMap<String, Point> apositions= new HashMap<String, ClassX.Point>();
	protected static HashMap<String, String> defs= new HashMap<String, String>();
	public static void loadPositions(HashMap<String, Point> set, String fName){
		BufferedReader r= null;
		try{
			File f= new File(fName);
			if (!f.exists()) return;
			r= new BufferedReader(new FileReader(f));
			String line;
			while ((line= r.readLine())!=null){
				String[] ll= line.split(";");
				if (ll.length<3) continue;
				if (line.trim().startsWith("x:")){
					String x="",y="", id="", bus="", vert="";
					for (String s : ll)
						if (s.trim().startsWith("x:")) x= s.split(":")[1];
						else if (s.trim().startsWith("y:")) y= s.split(":")[1];
						else if (s.trim().startsWith("id:")) id= s.split(":")[1];
						else if (s.trim().startsWith("bus:")) bus= s.split(":")[1];
						else if (s.trim().startsWith("vert:")) vert= s.split(":")[1];
					Point p= new Point(Double.parseDouble(x),Double.parseDouble(y));
					set.put(id, p); 
				} else {
					Point p= new Point(Double.parseDouble(ll[0].trim()),Double.parseDouble(ll[1].trim()));
					set.put(ll[2].trim(), p); }}
		} catch (Throwable t){
		} finally {
			if (r!=null) try { r.close(); } catch (Exception e) { e.printStackTrace(); }}}

	public static void loadDefs(String fName){
		BufferedReader r= null;
		try{
			File f= new File(fName);
			if (!f.exists()) return;
			r= new BufferedReader(new FileReader(f));
			String line;
			while ((line= r.readLine())!=null){
				int ix=line.indexOf(";");
				if (ix<0) continue;
				defs.put(line.substring(0,ix), line.substring(ix+1)); }
		} catch (Throwable t){
		} finally { if (r!=null) try { r.close(); } catch (Exception e) { e.printStackTrace(); }}

	}

	static String WATCH_PATH;

	public static void main(String[] args) throws Throwable {

		WATCH_PATH=new File(".").getCanonicalPath(); 


		loadPositions(positions, ".positions");

		loadPositions(apositions, ".apositions");

		loadDefs(".defs");

		//		for (Entry<String, Point> pos : positions.entrySet()){
		//			VClass cls= clss.get(pos.getKey());
		//			if (cls!=null) cls.pos= pos.getValue(); }
		//		for (Entry<String, Point> pos : apositions.entrySet()){
		//			VClass cls= clss.get(pos.getKey());
		//			if (cls!=null) cls.apos= pos.getValue(); }

		for (String s : args){
			File f= new File(s);
			if (!f.exists()) continue;
			if (f.isDirectory()) WATCH_PATH= f.getCanonicalPath();
			else WATCH_PATH= f.getParentFile().getCanonicalPath();
			analyzeFile(new File(s));
			System.err.println("===="+s); }

		for (Object cls : clss.values().toArray()){
			for (String dep : ((VClass)cls).deps){
				VClass depC= clss.get(dep);
				if (depC==null){
					VClass n= new VClass(dep.split(" @ ")[0].trim(), null, dep.split(" @ ")[1].trim(), new ArrayList<String>(), null, null, null);
					clss.put( n.fId, n);
					System.err.println("  adding missing "+dep ); }}}


		List<Entry<String, VClass>> clsse= new ArrayList<Map.Entry<String,VClass>>(clss.entrySet());
		Collections.sort(clsse, new Comparator<Map.Entry<String,VClass>>() {
			public int compare(Entry<String, VClass> o1, Entry<String, VClass> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});

		clss.clear();
		for (Map.Entry<String,VClass> e : clsse)
			clss.put(e.getKey(), e.getValue());

		for (VClass cls : clss.values()){
			//			System.err.println("\n"+ cls.id+(cls.sel==null?"":" @ "+cls.sel)+ " : "+ cls.src);
			//			for (String dep : cls.deps){
			//				VClass depC= clss.get(dep);
			//				System.err.println("   "+(depC==null?"??":depC.id)+" : "+dep );
			//			}
			if (cls.sel!=null)
				for (VClass selc : clss.values())
					if (selc!=cls)
						if (selc.tempF!=null){
							if (findSelPoint(selc.tempF, ".*<"+cls.sel+" [^>]+>.*|.*<"+cls.sel+">.*|.*<[^>]* "+ cls.sel+" [^>]*>.*")) 
								cls.sels.add(selc.fId);
						} else 
							if (selc.temp!=null 
							&&  selc.temp.replaceAll("\n|\r","").replaceAll("\"[^\"]*\"","").matches(".*<"+cls.sel+" [^>]+>.*|.*<"+cls.sel+">.*|.*<[^>]* "+ cls.sel+" [^>]*>.*")){ //contains("<"+cls.sel+">")){
								System.err.println("found selection link "+ cls.sel+" in "+selc.id);
								cls.sels.add(selc.fId);
							}
		}
		new ClassX();
	}


}