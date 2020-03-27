float angle(float3 a, float3 b) {
	return dot(a,b)/length(a)/length(b);
}


__kernel void raytrace(__global const float *rays, __global const float *p, __global float *result) {
	int i = get_global_id(0);
	float3 pos = (float3) (p[0],p[1],p[2]);
	float3 ray = (float3) (rays[3*i],rays[3*i+1],rays[3*i+2]);
	float sr = p[5];
	float srsq = sr*sr;
	float c = p[7];
	float GM = p[4];
	float dt = p[6];
	
	for(int it = 0; it < p[3]; it++) {
		float distancesq = dot(pos,pos);
		float distance = sqrt(distancesq);
		if(distancesq < srsq) {
			result[2*i] = -1;
			return;
		}
		
		float a = GM/distancesq;
		float3 normal = cross(ray,pos);
		float lennormal = length(normal);
		float dtheta = -a*dt/c* lennormal/length(ray) / fabs(distance-sr);				// -a*dt/c * sin(angle(ray,pos)) / abs(1-sr/distance)
		normal = normal/lennormal;
		
		float3 uwu = cross(normal,ray);
		ray = normal*dot(normal,ray) + cross(uwu,normal)*cos(dtheta) + uwu*sin(dtheta);		//rotate ray towards pos by angle dtheta
		ray = c*normalize(ray);
		pos = pos + ray*dt;
	}
	
	ray = normalize(ray);
	
	result[2*i] = asinpi(ray.y)+0.5;
	result[2*i+1] = 0.5-atan2pi(ray.z,ray.x)/2;
}
