import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSlider;

public class RayTracing extends Canvas {
	
	static final int WIDTH = 600, HEIGHT = 800;
	
	BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
	
	BufferedImage backgroundimage;
	int[] background;
	
	OpenCL opencl;
	
	public RayTracing() throws IOException {
		JFrame frame = new JFrame("Ray Tracing");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				opencl.clean();
				System.exit(0);
			}
		});
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.add(this);
		JSlider focallength = new JSlider(1, 150, 40);
		focallength.addChangeListener(e -> {
			focal = Math.tan(Math.PI*focallength.getValue()/360d)*WIDTH/2;
			genRays(focal);
			System.out.println(focallength.getValue());
		});
		frame.add(focallength,BorderLayout.NORTH);
//		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true);
		
		genRays(focal);
		
		opencl = new OpenCL(this);
		opencl.initAll();
		opencl.createRays();
		
		backgroundimage = ImageIO.read(getClass().getResourceAsStream("/Hip2DR2_2k300Func3Max3.jpg"));
		background = new int[backgroundimage.getWidth()*backgroundimage.getHeight()];
		backgroundimage.getRGB(0, 0, backgroundimage.getWidth(), backgroundimage.getHeight(), background, 0, backgroundimage.getWidth());

		new Thread(() -> run(), "Render").start();
	}
	
	double focal = Math.tan(Math.PI/9)*WIDTH/2; //FOV = 40º
	
	Vec3[] rays = new Vec3[WIDTH*HEIGHT];
	
	public void genRays(double focal) {
		for(int y = 0; y < HEIGHT; y++) {
			int y0 = y - HEIGHT/2;
			for(int x = 0; x < WIDTH; x++) {
				int x0 = x - WIDTH/2;
				rays[x+y*WIDTH] = new Vec3(x0,y0,focal).normalize();
			}
		}
	}
	
	public void run() {
		createBufferStrategy(3);
		while(true) {
			render();
		}
	}
	
	
//	public void shootRays() {
//		draw = true;
//		
//		Vec3 bhpos = new Vec3();
//		double bhmass = 1.989e30*4e6;
//		double G = 6.67408/(1e11);
//		double c = 299792458;
//		double dt = 0.025;
//		double GM = G*bhmass;
//		double srsq = Math.pow(2*GM/c/c, 2);
////		System.out.println(Math.sqrt(srsq));
//		
////		shootRay(bhpos, srsq, c, dt, GM, rays[166+32*WIDTH]);
//		
//		for(int i = 120*WIDTH; i < rays.length; i++) {
//			Vec3 ray = shootRay(bhpos, srsq, c, dt, GM, rays[i].clone());
////			System.out.println(ray);
////			System.exit(0);
////			if(i%300==0) System.out.println(i);
//			
//			if(ray == null) {
//				System.out.println(i%300 + ", " + i/300);
//				pixels[i] = 0xff000000;
//				continue;
//			}
//			int latitude = (int) ((Math.asin(ray.y)+Math.PI/2)/Math.PI*backgroundimage.getHeight());
//			int longitude = (int)((1-(Math.atan2(ray.z,ray.x)+Math.PI)/(2*Math.PI))*backgroundimage.getWidth());
////			if(latitude >= backgroundimage.getHeight() || longitude >= backgroundimage.getWidth()) continue;
//			pixels[i] = background[longitude+latitude*backgroundimage.getWidth()];
//			
//		}
//	}
//	
//	private Vec3 shootRay(Vec3 bhpos, double srsq, double c, double dt, double GM, Vec3 ray) {
//		Vec3 pos = new Vec3(0, 0, -4e10);
//		for(int step = 0; step < 6400; step++) {
//			Vec3 r = bhpos.sub(pos);
//			double distancesq = r.modSq();
//			double distance = Math.sqrt(distancesq);
//			if(distancesq < srsq) {
//				System.out.println(distance);
//				System.out.println("out");
//				return null;
//			}
//			System.out.println(distance);
//			double a = GM/distancesq;
//			double dTheta = -a*dt/c*Math.sin(Vec3.angle(ray, pos));
//			dTheta /= Math.abs(1-2*GM/(distance*c*c));
//			
//			//Rotate
//			Vec3 cross = Vec3.cross(ray, r).normalize();
//			double cos = Math.cos(dTheta);
//			double sin = Math.sin(dTheta);
//			
//			Vec3 ux;
//			
//			ray = cross.scale(cross.dot(ray)).add(Vec3.cross(ux = Vec3.cross(cross, ray), cross).scale(cos)).add(ux.scale(sin)).setMag(c);
//			pos = pos.add(ray.scale(dt));
//			
//		}
//		ray = ray.normalize();
////		System.out.println(ray);
//		return ray;
//	}

	int i = 0;
	
	public void render() {
		Graphics g = getBufferStrategy().getDrawGraphics();
		
		if(opencl.params[2] < -1.20e10) {
			opencl.params[2] *= 0.99f;
			System.out.println(opencl.params[2]);
			opencl.loadBlackHoleParameters();
			opencl.shootRays();
			
			for(int i = 0; i < pixels.length; i++) {
				int latitude = (int) (opencl.result[2*i]*backgroundimage.getHeight());
				int longitude = (int) (opencl.result[2*i+1]*backgroundimage.getWidth());
				if(opencl.result[2*i] == -1) pixels[i] = 0xff000000; // latitude = -1 means lightray is inside the black hole
				else pixels[i] = background[longitude+latitude*backgroundimage.getWidth()];
			}			
			try {
				ImageIO.write(image, "png", new File(i+++".png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		for(int i = 0; i < rays.length; i++) {
//			int latitude = (int) ((Math.asin(rays[i].y)+Math.PI/2)/Math.PI*backgroundimage.getHeight());
//			int longitude = (int)((1-(Math.atan2(rays[i].z,rays[i].x)+Math.PI)/(2*Math.PI))*backgroundimage.getWidth());
////			if(latitude >= backgroundimage.getHeight() || longitude >= backgroundimage.getWidth()) continue;
//			pixels[i] = background[longitude+latitude*backgroundimage.getWidth()];
//		}
		
		g.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
		
		g.dispose();
		getBufferStrategy().show();
	}
	
	public static void main(String[] args) throws IOException {
		new RayTracing();
	}
}