package it.unipi.dii.cws;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.StringTokenizer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class SvmLoader {
	
	public static class Elem {
		
		public final int id;
		public final double weight;

		public Elem(final int id) {
			
			this(id, 1.0);
		}		

		public Elem(final int id, final double weight) {
			
			this.id = id;
			this.weight = weight;
		}		
	}

	private ObjectList<Elem> vec = null;
	private String line = null;
	private boolean is_labeled = false;
	private boolean is_weighted = false;
	
	private BufferedReader reader = null;
	
	public SvmLoader(final String input_file_name, final boolean is_weighted, final boolean is_labeled) throws FileNotFoundException {
		
		this.reader = new BufferedReader(new FileReader(input_file_name));
		this.is_labeled = is_labeled;
		this.is_weighted = is_weighted;
		
		this.vec = new ObjectArrayList<Elem>();
	}
	
	public boolean next() {
		
		if (this.reader == null)
			return false;
		
		vec.clear();
		
		
		try {
			if ((line = reader.readLine()) == null) {
				this.reader.close();
				this.reader = null; 
				return false;
			}
		} catch (IOException e1) {
			try {
				this.reader.close();
			} catch (IOException e2) {
				e1.printStackTrace();
			}
			this.reader = null; 
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(line," \t\n\r\f");

		if (is_labeled) // we consume the label
			st.nextToken();

		String tok;
		while (st.hasMoreElements()) {
			tok = st.nextToken();
			if (is_weighted) {
				vec.add(new Elem(Integer.parseInt(tok.split(":")[0]), 
							     Double.parseDouble(tok.split(":")[1]))
				);
			} else {
				vec.add(new Elem(Integer.parseInt(tok)));
			}
		}
		return true;
	}
	
	public Elem[] get() {
		
		return this.vec.toArray(new Elem[0]);
	}

	public static Elem[][] load(final String fn, final boolean is_weighted, final boolean is_labeled) throws FileNotFoundException
	{
		ObjectList<Elem[]> vecs = new ObjectArrayList<>();
		SvmLoader in = new SvmLoader(fn, is_weighted, is_labeled);
		
		while (true) {
    		if (!in.next())
    			break;
    		vecs.add(in.get());
		}
		
		return vecs.toArray(new Elem[0][]);
	}

}
