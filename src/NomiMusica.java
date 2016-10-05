/**
 * Copyright (C) 2002-2006 Bas Peters
 * 
 * This file is part of MARC4J
 * 
 * MARC4J is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * MARC4J is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MARC4J; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;

public class NomiMusica
{
	private final static Logger log = Logger.getLogger("LOG");
	private final static Logger console = Logger.getLogger("CONSOLE");

	private static String clean(String data)
	{
		data = data.replace("\u00c2\u0089", "");
		data = data.replace("\u00c2\u0088", "");
		return data;
	}

	public TreeSet<String> extract(File file)
	{
		InputStream input = null;
		String bid = null;
		TreeSet<String> set = new TreeSet<String>();
		try
		{
			input = new GZIPInputStream(new FileInputStream(file));
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		MarcReader reader = new MarcStreamReader(input);
		while(reader.hasNext())
		{
			Record record = reader.next();

// Prima rileva il tipo di materiale (ci servono solo a, b e c)

			char type = record.getLeader().toString().charAt(6);

// Legge il BID che è essenziale ai fini del debugging.

			bid = ((ControlField) record.getVariableField("001")).getData();

// A seconda del type prendiamo diversi campi e sottocampi

			switch(type)
			{

/*
 * Nel caso type = 'c' testiamo due date in 100$a, dopo averle ripulite
 */

				case 'c':
					String data = ((DataField) record.getVariableField("100")).getSubfield('a').getData();
					if(data != null && data != "")
					{
						String data1 = data.substring(9, 13).trim().replace(".", "0").replace("/f", "00");
						String data2 = data.substring(13, 17).trim().replace(".", "0").replace("/f", "00");
						if(data1 == null || data1.equals(""))
						{
							data1 = "0000";
						}
						if(data2 == null || data2.equals(""))
						{
							data2 = "0000";
						}
						if(Integer.parseInt(data1) < 1850 && Integer.parseInt(data2) < 1850)
						{
							log.debug("[" + data.substring(9, 13) + "] -> " + data1);
							log.debug("[" + data.substring(13, 17) + "] -> " + data2);

/*
 * Arrivati qui, il record è adatto a estrarre i nomi. Si prendono diversi 700.
 * L'estrazione vera e propria andrebbe demandata ad un apposito metodo, perché
 * servirà negli altri casi
 */

							Iterator<VariableField> dfIter;
							String[] f700 = { "700", "701", "702", "710", "711", "712" };
							dfIter = ((List<VariableField>) record.getVariableFields(f700)).iterator();
							while(dfIter.hasNext())
							{
								DataField df = (DataField) dfIter.next();
								String tag = df.getTag();
								data = df.getSubfield('a').getData();
								if(df.getSubfield('b') != null)
								{
									data += df.getSubfield('b').getData();
								}
								else
								{
									log.warn(bid + ": " + tag + "$b nullo");
								}
								set.add(clean(data));
							}
						}
					}
					break;
				default:
					break;
			}
		}
		try
		{
			input.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return set;
	}

	public File[] fileArray(File file)
	{
		File[] files = null;
		if(file.isFile())
		{
			files = new File[] { file };
		}
		else
		{
			files = file.listFiles(new FilenameFilter()
			{

				@Override
				public boolean accept(File dir, String name)
				{
					if(name.endsWith("mrc.gz"))
						return true;
					else
						return false;
				}
			});
		}
		return files;
	}

	public static void main(String args[]) throws Exception
	{
		SimpleLayout sl = new SimpleLayout();
		ConsoleAppender ca = new ConsoleAppender(sl, "System.out");
		console.setLevel(Level.INFO);
		console.addAppender(ca);
		WriterAppender wa = null;
		wa = new WriterAppender(sl, new PrintWriter("nomi.log"));
		log.addAppender(wa);
		log.setLevel(Level.INFO);
		NomiMusica tu = new NomiMusica();
		File[] files = tu.fileArray(new File(args[0]));
		Arrays.sort(files);
		TreeSet<String> tree = new TreeSet<String>();
		StopWatch sw = new StopWatch();
		sw.start();
		int count = 0;
		for(File file : files)
		{
			console.info("Elaborazione file " + file.getName() + " (" + ++count + "/" + files.length + ")");
			tree.addAll(tu.extract(file));
			sw.split();
			console.info(sw.toSplitString() + " (" + tree.size() + " nomi trovati finora)");
			sw.unsplit();
		}

		FileOutputStream fos = new FileOutputStream("nomi.txt");
		OutputStreamWriter osw = new OutputStreamWriter(fos, "ISO-8859-1");
		PrintWriter pw = new PrintWriter(osw);
		for(String val : tree)
		{
			pw.println(val);
		}
		pw.flush();
		pw.close();
		sw.stop();
		console.info("Tempo impiegato: " + sw.toString());
	}
}
