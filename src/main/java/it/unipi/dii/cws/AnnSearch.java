package it.unipi.dii.cws;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;


public class AnnSearch 
{
	public static class IdErrs implements Comparable<IdErrs>
	{
		public final int id;
		public final int errs;
		
		public IdErrs(final int id, final int errs)
		{
			this.id = id;
			this.errs = errs;
		}

		@Override
		public int compareTo(IdErrs that) 
		{
			if (this.errs != that.errs) {
                return Integer.compare(this.errs, that.errs); // ascending order
            }
            return Integer.compare(this.id, that.id); // ascending order		
		}
		
		@Override
		public String toString()
		{
			return this.id + ":" + this.errs; 
		}
	}

	public static final class Args 
	{
	    // required arguments

	    @Option(name = "-i",  metaVar = "[String]", required = true, usage = "input file name of database of CWS-sketches (in bvecs format)")
	    public String base_fn;

	    @Option(name = "-q",  metaVar = "[String]", required = true, usage = "input file name of queries of CWS-sketches (in bvecs format)")
	    public String query_fn;

	    @Option(name = "-o",  metaVar = "[String]", required = true, usage = "output file name of ranked score data")
	    public String score_fn;

	    // optional arguments
	    
	    @Option(name = "-k", metaVar = "[int]", required = false, usage = "k-nearest neighbors")
	    public int topk = 100;
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
		int[][] base_codes  = Util.loadBinVectors(args.base_fn);
		int[][] query_codes = Util.loadBinVectors(args.query_fn);
		
		IdErrs[] ranked_scores = new IdErrs[base_codes.length];
		
		PrintWriter ofs = new PrintWriter(args.score_fn);
		ofs.print(query_codes.length + '\n' + args.topk + '\n');

		for (int[] query: query_codes) {
			
			IntStream.range(0, base_codes.length).parallel().forEach(i -> { int[] base = base_codes[i]; ranked_scores[i] = new IdErrs(i, Util.hammingDistance(base, query)); });
			Arrays.sort(ranked_scores);
			
			Arrays.stream(ranked_scores).limit(args.topk).forEach( e -> { ofs.print(e + ","); });
			
			ofs.println();
		}
		
		ofs.close();
		System.out.println("Output in " + args.score_fn);
	}

}
