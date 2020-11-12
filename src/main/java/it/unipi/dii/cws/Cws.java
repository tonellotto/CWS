package it.unipi.dii.cws;

import java.io.DataOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import it.unimi.dsi.util.SplitMix64Random;

import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;

public class Cws 
{
	protected static final int BUFFER_VECS = 128 * 1024;
	
	public static final class Args 
	{
	    // required arguments

	    @Option(name = "-i",  metaVar = "[String]", required = true, usage = "input file name of database vectors (in ASCII format)")
	    public String input_fn;

	    @Option(name = "-o",  metaVar = "[String]", required = true, usage = "output file name of CWS-sketches (in ASCII format)")
	    public String output_fn;

	    @Option(name = "-d",  metaVar = "[int]", required = true, usage = "dimension of the input data")
	    public int dat_dim;

	    // optional arguments
	    
	    @Option(name = "-D", metaVar = "[int]", required = false, usage = "cws_dim")
	    public int cws_dim = 64;

	    @Option(name = "-w", metaVar = "[boolean]", required = false, usage = "Does the input data have weight?")
	    public boolean weighted = false;

	    @Option(name = "-l", metaVar = "[boolean]", required = false, usage = "Does each input vector have a label at the head?")
	    public boolean labeled = false;

	    @Option(name = "-s", metaVar = "[int]", required = false, usage = "seed for random matrix data")
	    public int seed = 0;
	}
		
	public static void main(String[] argv) throws InterruptedException, IOException
	{
		Args args = new Args();
		CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));
		try {
			parser.parseArgument(argv);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			return;
		}

		run(args);
	}

	private static void run(Args args) throws InterruptedException, IOException 
	{
		System.out.println("1) Generate random matrix data...");
		
		double[] R = new double[args.dat_dim * args.cws_dim];
		double[] C = new double[args.dat_dim * args.cws_dim];
		double[] B = new double[args.dat_dim * args.cws_dim];
		
		Random seeder = new SplitMix64Random(args.seed);
		
	    final long seed_R = seeder.nextLong();
	    final long seed_C = seeder.nextLong();
	    final long seed_B = seeder.nextLong();
	    
	    long start_time = System.currentTimeMillis();
	    
	    ExecutorService executorService = Executors.newFixedThreadPool(3); // number of threads
	    
	    executorService.submit(() -> { Util.generateRandomMatrix(new GammaDistribution(2.0, 1.0), R, seed_R); });
	    executorService.submit(() -> { Util.generateRandomMatrix(new GammaDistribution(2.0, 1.0), C, seed_C); });
	    executorService.submit(() -> { Util.generateRandomMatrix(new UniformRealDistribution(0.0, 1.0), B, seed_B); });
	    
	    executorService.shutdown();
	    executorService.awaitTermination(12, TimeUnit.HOURS);
	    
	    long end_time = System.currentTimeMillis();
	    
	    System.out.println("Elapsed time: " + (new SimpleDateFormat("mm:ss.SSS")).format(new Date(end_time-start_time)));
	    double MiB = Double.BYTES * R.length * 3 / (1024.0 * 1024.0);
	    System.out.println("The random matrix data consumes " + String.format("%.02f", MiB) + " MiB");

	    System.out.println("2) Do consistent weighted sampling...");
	    
	    SvmLoader in = new SvmLoader(args.input_fn, args.weighted, args.labeled);
	    DataOutputStream out = Util.makeOutputStream(args.output_fn);
	    
	    SvmLoader.Elem[][] in_buffer = new SvmLoader.Elem[BUFFER_VECS][];
	    int[] out_buffer = new int[BUFFER_VECS * args.cws_dim];
	    
	    long processed = 0;
	    start_time = System.currentTimeMillis();
	    
	    while (true) {
	    	// Bulk loading
	    	int num_vecs = 0;
	    	while (num_vecs < BUFFER_VECS) {
	    		if (!in.next())
	    			break;
	    		in_buffer[num_vecs++] = in.get();
	    	}
	    	if (num_vecs == 0)
	    		break;
	    	
	    	// Sampling
	    	IntConsumer sampling = id -> {
	    		SvmLoader.Elem[] data_vec = in_buffer[id];
	    		
	    		for (int i = 0; i < args.cws_dim; ++i) {
	    			int begin_pos = i * args.dat_dim;
	    			double min_a = Double.POSITIVE_INFINITY;
	    			int min_id = 0;
	    			for (SvmLoader.Elem feat: data_vec) {
	    				int j = feat.id;
	    				
	    				double t = Math.floor(Math.log10(feat.weight) / R[begin_pos + j] + B[begin_pos + j]);
	    				double a = Math.log10(C[begin_pos + j]) - (R[begin_pos + j] * (t + 1.0 - B[begin_pos + j]));
	    				
	    				if (a < min_a) {
	    					min_a = a;
	    					min_id = j;
	    				}
	    			}
	    			if (args.dat_dim <= min_id) {
	    				System.err.println("error: min_id exceeds data_dim");
	    				System.exit(-1);
	    			}
	    			
	    			out_buffer[id * args.cws_dim + i] = min_id;
	    		}
	    	};
	    	
	    	IntStream.range(0, num_vecs).parallel().forEach(sampling);
	    	
	    	// Writing
	    	for (int id = 0; id < num_vecs; ++id) {
	    		out.writeInt(args.cws_dim);
	    		for (int i = 0; i < args.cws_dim; ++i)
	    			out.writeInt(out_buffer[id * args.cws_dim + i]);
	    	}
	    	
	    	processed += num_vecs;
	    	
		    end_time = System.currentTimeMillis();
		    System.out.println(processed + " vectors processed in " + (new SimpleDateFormat("mm:ss.SSS")).format(new Date(end_time-start_time)));
	    }
	    
	    out.close();
	    
	    end_time = System.currentTimeMillis();
	    double M = processed / (1000.0 * 1000.0);
	    System.out.println("Completed!! Processed " + String.format("%.02f", M) + " millions of elements in " + (new SimpleDateFormat("mm:ss.SSS")).format(new Date(end_time-start_time)));
	}
}
