public class Vec3 {
	public double x,y,z;
	
	public Vec3() {}
	
	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec3 scale(double s) {
		return new Vec3(x*s, y*s, z*s);
	}
	
	public Vec3 clone() {
		return new Vec3(x,y,z);
	}
	
	public Vec3 sub(Vec3 v) {
		return new Vec3(x-v.x,y-v.y,z-v.z);
	}

	public double mod() {
		return Math.sqrt(x*x+y*y+z*z);
	}

	public static double angle(Vec3 v, Vec3 w) {
		return v.dot(w)/v.mod()/w.mod();
	}
	
	public static Vec3 cross(Vec3 v, Vec3 w) {
		return new Vec3(v.y*w.z-v.z*w.y,v.z*w.x-v.x*w.z,v.x*w.y-v.y*w.x);
	}
	
	public double dot(Vec3 v) {
		return v.x*this.x+v.y*this.y+v.z*this.z;
	}
	
	public double modSq() {
		return x*x+y*y+z*z;
	}
	
	public Vec3 normalize() {
		double mod = mod();
		if(mod != 0) return scale(1/mod);
		return new Vec3();
	}
	
	public double dist(Vec3 v) {
		double x = this.x - v.x;
		double y = this.y - v.y;
		double z = this.z - v.z;
		return Math.sqrt(x*x+y*y+z*z);
	}

	public Vec3 add(Vec3 v) {
		return new Vec3(x+v.x,y+v.y,z+v.z);
	}

	public Vec3 setMag(double s) {
		double mod = mod();
		if(mod!=0) return scale(s/mod);
		return new Vec3();
	}
	
	public String toString() {
		return x + "," + y + "," + z;
	}
}
