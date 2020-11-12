package it.unipi.dii.cws;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

public class GroundTruth 
{
	private static class IdSim implements Comparable<IdSim>
	{
		public final int id;
		public final double sim;
		
		public IdSim(final int id, final double sim)
		{
			this.id = id;
			this.sim = sim;
		}

		@Override
		public int compareTo(IdSim that) {
			if (this.sim != that.sim) {
                return Double.compare(that.sim, this.sim); // descending order
            }
            return Integer.compare(this.id, that.id); // ascending order		
        }
		
		@Override
		public String toString()
		{
			return this.id + ":" + this.sim; 
		}
	}

	public static final class Args 
	{
	    // required arguments

	    @Option(name = "-i",  metaVar = "[String]", required = true, usage = "input file name of database vectors (in ASCII format))")
	    public String base_fn;

	    @Option(name = "-q",  metaVar = "[String]", required = true, usage = "input file name of query vectors (in ASCII format)")
	    public String query_fn;

	    @Option(name = "-o",  metaVar = "[String]", required = true, usage = "output file name of the ground truth")
	    public String groundtruth_fn;

	    // optional arguments
	    
	    @Option(name = "-w", metaVar = "[boolean]", required = false, usage = "Does the input data have weight?")
	    public boolean weighted = false;

	    @Option(name = "-l", metaVar = "[boolean]", required = false, usage = "Does each input vector have a label at the head?")
	    public boolean labeled = false;

	    @Option(name = "-k", metaVar = "[int]", required = false, usage = "k-nearest neighbors")
	    public int topk = 100;

	    @Option(name = "-p", metaVar = "[int]", required = false, usage = "step of printing progress")
	    public int progress = 100;
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

	private static void run(Args args) throws FileNotFoundException 
	{
		SvmLoader.Elem[][] base_vecs  = SvmLoader.load(args.base_fn, args.weighted, args.labeled);	
		SvmLoader.Elem[][] query_vecs = SvmLoader.load(args.query_fn, args.weighted, args.labeled);
		
		IdSim[] id_sims = new IdSim[base_vecs.length];
		
		PrintWriter ofs = new PrintWriter(args.groundtruth_fn);
		ofs.print(query_vecs.length + '\n' + args.topk + '\n');
		
		long start_time = System.currentTimeMillis();

		int processed = 0;
		for (SvmLoader.Elem[] query: query_vecs) {
			if (processed > 0 && processed % args.progress == 0)
				System.out.println(processed + " queries processed in " + (new SimpleDateFormat("mm:ss.SSS")).format(new Date(System.currentTimeMillis() - start_time))); 
			
			IntStream.range(0, base_vecs.length).parallel().forEach(i -> { SvmLoader.Elem[] base = base_vecs[i]; id_sims[i] = new IdSim(i, Util.minMaxSimilarity(base, query)); });

			Arrays.sort(id_sims);
			
			Arrays.stream(id_sims).limit(args.topk).forEach( e -> { ofs.print(e + ","); });

			ofs.println();
			processed++;
		}
		
		ofs.close();
		
		long end_time = System.currentTimeMillis();
		System.out.println("Completed!! Processed " + query_vecs.length + " queries in " + (new SimpleDateFormat("mm:ss.SSS")).format(new Date(end_time-start_time)));
		System.out.println("Output in " + args.query_fn);
	}
}
