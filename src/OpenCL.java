

import static org.jocl.CL.*;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class OpenCL {
	
	cl_context context;
	cl_command_queue command_queue;
	
	private cl_mem memObjects[];
	private cl_kernel kernel[];
	private cl_program program;
	
	private RayTracing main;
	
	public OpenCL(RayTracing rm) {
		main = rm;
		init();
	}
	
	public void init() {
		//0 = nvidia, 1 = intel graphics, 2 = i7 4510U
		createContext(0);

		String programSrc = loadSrc("/raytracing.cl");
		System.out.println(programSrc);
		program = clCreateProgramWithSource(context, 1, new String[] {programSrc}, null, null);
		clBuildProgram(program, 0, null, null, null, null);
	}
	
	public void clean() {
		for(int a = 0; a < memObjects.length; a++) {
			clReleaseMemObject(memObjects[a]);
		}
		for(int a = 0; a < kernel.length; a++) {
			if(kernel[a] == null) continue;
			clReleaseKernel(kernel[a]);			
		}
		clReleaseProgram(program);
		clReleaseCommandQueue(command_queue);
		clReleaseContext(context);
	}
	
	public void shootRays() {
		int n = main.pixels.length;
		
		clEnqueueNDRangeKernel(command_queue, kernel[0], 1, null, new long[] {n}, new long[] {800}, 0, null, null);
		clEnqueueReadBuffer(command_queue, memObjects[2], CL_TRUE, 0, result.length*Sizeof.cl_float, _result, 0, null, null);
	}
	
	float[] rays;
	float[] params;
	float[] result;
	Pointer _rays;
	Pointer _params;
	Pointer _result;
	
	public void initAll() {
		int n = main.pixels.length;
		
		double bhmass = 1.989e30*4e6;
		double G = 6.67408/(1e11);
		double c = 299792458;
		float dt = 0.05f;
		double GM = G*bhmass;
		float sr = (float) (2*GM/c/c);
		
		rays = new float[3*n];
		//					  ray pos			steps	GM			Schwarz. radius		dt			c
		params = new float[] {0, 0, -4e10f,		2000,	(float)GM,	sr,				dt,		(float)c};
		result = new float[2*n];
		System.out.println(result.length);
		createRays();

		_rays = Pointer.to(rays);
		_params = Pointer.to(params);
		_result = Pointer.to(result);
		
		memObjects = new cl_mem[3];
		memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, rays.length*Sizeof.cl_float, _rays, null);
		memObjects[1] = clCreateBuffer(context, CL_MEM_READ_ONLY, params.length*Sizeof.cl_float, _params, null);
		memObjects[2] = clCreateBuffer(context, CL_MEM_WRITE_ONLY, result.length*Sizeof.cl_float, _result, null);
		
		kernel = new cl_kernel[] {clCreateKernel(program, "raytrace", null)};

		clSetKernelArg(kernel[0], 0, Sizeof.cl_mem, Pointer.to(memObjects[0])); //rays
		clSetKernelArg(kernel[0], 1, Sizeof.cl_mem, Pointer.to(memObjects[1])); //params
		clSetKernelArg(kernel[0], 2, Sizeof.cl_mem, Pointer.to(memObjects[2])); //lat,long
		
		loadBlackHoleParameters();
	}
	
	public void loadBlackHoleParameters() {
		clEnqueueWriteBuffer(command_queue, memObjects[1], CL_TRUE, 0, params.length*Sizeof.cl_float, _params, 0, null, null);
	}
	
	public void createRays() {
		int n = main.pixels.length;
		for(int i = 0; i < n; i++) {
			rays[3*i] = (float) main.rays[i].x;
			rays[3*i+1] = (float) main.rays[i].y;
			rays[3*i+2] = (float) main.rays[i].z;
		}
		if(memObjects!= null) {
			clEnqueueWriteBuffer(command_queue, memObjects[0], CL_TRUE, 0, rays.length*Sizeof.cl_float, _rays, 0, null, null);
		}
		
	}
	
	public String loadSrc(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get("C:/Users/Usuario/workspace/3D RENDERING/Black holes/res"+path)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void createContext(int dev) {

		final int platformIndex = dev == 0 ? 0:1;
        final long deviceType = dev == 2 ? CL_DEVICE_TYPE_CPU:CL_DEVICE_TYPE_GPU;
        final int deviceIndex = 0;
		
        CL.setExceptionsEnabled(true);
        
		int numPlatformsArray[] = new int[1];
		clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray[0];
		
		cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
		clGetPlatformIDs(platforms.length, platforms, null);
		cl_platform_id platform = platforms[platformIndex];
		
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
		
		
		int[] numDevicesArray = new int[1];
		clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];
		
		cl_device_id[] devices = new cl_device_id[numDevices];
		clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
		cl_device_id device = devices[deviceIndex];
		
		long[] length = new long[1];
		byte[] buffer = new byte[2000];
		clGetDeviceInfo(device, CL_DEVICE_NAME, buffer.length, Pointer.to(buffer), length);
		System.out.println(new String(buffer).trim());
//		buffer = new byte[1000];
//		clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_GROUP_SIZE, buffer.length, Pointer.to(buffer), length);

//		System.out.println("Max work group size: " + new String(buffer).trim());
		context = clCreateContext(contextProperties, 1, new cl_device_id[] {device}, null, null, null);
		command_queue = clCreateCommandQueue(context, device, 0, null);
	}
}
