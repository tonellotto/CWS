package it.unipi.dii.cws;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.math3.distribution.RealDistribution;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class Util {
	
	public static DataInputStream makeInputStream(final String filename) {
		
		try {
			return new DataInputStream(Files.newInputStream(Paths.get(filename)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public static DataOutputStream makeOutputStream(final String filename) {
		
		try {
			return new DataOutputStream(Files.newOutputStream(Paths.get(filename)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public static void generateRandomMatrix(RealDistribution dist, double[] out, long seed) {
		
		dist.reseedRandomGenerator(seed);
		System.arraycopy(dist.sample(out.length), 0, out, 0, out.length);
	}	
	
	public static double minMaxSimilarity(final SvmLoader.Elem[] x, final SvmLoader.Elem[] y)
	{
		double min_sum = 0.0;
		double max_sum = 0.0;
		
		int i = 0, j = 0;
		while (i < x.length && j < y.length) {
			if (x[i].id == y[j].id) {
				if (x[i].weight < y[j].weight) {
					min_sum += x[i].weight;
					max_sum += y[j].weight;
				} else {
					min_sum += y[j].weight;
					max_sum += x[i].weight;
				}
				++i;
				++j;
			} else if (x[i].id < y[j].id) {
				max_sum += x[i].weight;
				++i;
			} else {
				max_sum += y[j].weight;
				++j;
			}
		}
		
		for (; i < x.length; ++i)
			max_sum += x[i].weight;
		
		for (; j < y.length; ++j)
			max_sum += y[j].weight;
		
		return min_sum / max_sum;
	}

	public static int hammingDistance(final int[] v1, final int[] v2)
	{
		int errs = 0;
		for (int i = 0; i < v1.length; ++i)
			if (v1[i] != v2[i])
				errs++;
		return errs;
	}

	public static int[][] loadBinVectors(final String fn) throws FileNotFoundException
	{
		ObjectList<int[]> vecs = new ObjectArrayList<>();
		DataInputStream in = makeInputStream(fn);

		int cws_dim;
		int[] v;
		
		try {
			while (true) {
				cws_dim = in.readInt();
				v = new int[cws_dim];
				for (int i = 0; i < cws_dim; ++i)
					v[i] = in.readInt();
				vecs.add(v);
			}
		} catch (IOException __) {}

		return vecs.toArray(new int[0][]);
	}

}
