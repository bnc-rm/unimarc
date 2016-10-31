/**
 * Copyleft Andrea Giuliano (ICCU, Italian Ministry per Cultural Heritage) Do
 * whatever you want with this code
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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
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
/*
 * Si creano dei TreeSet con opportuno ordinamento per contenere tutti le coppie
 * (VID, nome) relativamente a diversi tipi di materiale. Alla fine saranno fusi
 * insieme, ma ci conviene averli anche separati per le esigenze di pulizia in
 * Indice
 */
	private static TreeSet<String> aSet = new TreeSet<String>(new Comp());
	private static TreeSet<String> bSet = new TreeSet<String>(new Comp());
	private static TreeSet<String> cSet = new TreeSet<String>(new Comp());
	private static TreeSet<String> dSet = new TreeSet<String>(new Comp());

/*
 * Pulisce una stringa da caratteri non-sort
 */
	private static String clean(String data)
	{
		data = data.replace("\u00c2\u0089", "");
		data = data.replace("\u00c2\u0088", "");
		return data;
	}

/*
 * Serve un comparatore che escluda il VID che occupa la parte iniziale delle
 * stringhe
 */
	private static class Comp implements Comparator<String>
	{
		@Override
		public int compare(String o1, String o2)
		{
			if(o1.length() > 10 && o2.length() > 10)
			{
				String n1 = o1.substring(10);
				String n2 = o2.substring(10);
				return(n1.compareTo(n2));
			}
/*
 * Raramente capitano VID senza nome. In questo caso il confronto è standard
 */
			else
			{
				log.info("nome1: [" + o1 + "]");
				log.info("nome2: [" + o2 + "]");
				return o1.compareTo(o2);
			}
		}
	}

/*
 * Dato un record, ritorna l'insieme dei nomi unici in esso eventualmente
 * contenuti in vari campi dell'area 700.
 */
	private static Vector<String> getNames(Record record, String bid)
	{
		Iterator<VariableField> dfIter;
		String[] f700 = { "700", "701", "702", "710", "711", "712" };
		dfIter = ((List<VariableField>) record.getVariableFields(f700)).iterator();
		Vector<String> names = new Vector<String>();
		while(dfIter.hasNext())
		{
			DataField df = (DataField) dfIter.next();
// String data = df.getSubfield('3').getData().replace("\\",
// "").replace("ITICCU", "");
			String data = df.getSubfield('3').toString().replace("\\", "").replace("ITICCU", "");
			Iterator<Subfield> sfIter = df.getSubfields().iterator();
			while(sfIter.hasNext())
			{
				Subfield sf = sfIter.next();
				char code = sf.getCode();
				String sfText = sf.toString();
				switch(code)
				{
					case '3':
						break;
					case '4':
						break;
					case 'a':
						sfText = sfText.replaceFirst(" : ", " ");
						data += sfText;
						break;
					case 'b':
						sfText = sfText.replaceFirst(", ", "");
						sfText = sfText.replaceFirst(" : ", " ");
						data += sfText;
						break;
					case 'c':
						sfText = sfText.replaceFirst(" <", "");
						sfText = sfText.replaceFirst(">$", "");
						sfText = sfText.replaceFirst(" ; ", "");
						data += sfText;
						break;
					case 'd':
						sfText = sfText.replaceFirst(" <", "");
						sfText = sfText.replaceFirst(">$", "");
						sfText = sfText.replaceFirst(" ; ", "");
						data += sfText;
						break;
					case 'f':
						sfText = sfText.replaceFirst(" <", "");
						sfText = sfText.replaceFirst(">$", "");
						sfText = sfText.replaceFirst(" ; ", "");
						data += sfText;
						break;

					default:
						data += sf.toString();
						break;
				}
			}
			names.add(clean(data));
		}
		return names;
	}

/*
 * Esporta in un file uno degli insiemi prodotti
 */
	private void output(TreeSet<String> set, String file)
	{
		try
		{
			FileOutputStream fos = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "ISO-8859-1");
			PrintWriter pw = new PrintWriter(osw);
			for(String val : set)
			{
				pw.println(val);
			}
			pw.flush();
			pw.close();
			osw.close();
			fos.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

/*
 * Apre un file mrc.gz oppure mrc e scorre tutti i record alla ricerca di quelli
 * adatti all'estrazione di nomi legati alla musica. Tutti i nomi sono inseriti
 * in appositi set statici
 */
	public void extract(File file)
	{
		InputStream input = null;
		String bid = null;

		try
		{
			if(file.getName().endsWith("mrc.gz"))
			{
				input = new GZIPInputStream(new FileInputStream(file));
			}
			if(file.getName().endsWith("mrc"))
			{
				input = new FileInputStream(file);
			}
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

			String data;

/*
 * Nei casi type = 'a|b' testiamo 105$a/11 = 'i' || 140$a/17-18 = 'da'. Gli
 * eventuali nomi saranno però tenuti separati per essere estratti anche in file
 * distinti
 */

			if(type == 'a' || type == 'b')
			{
				DataField f105 = (DataField) record.getVariableField("105");
				DataField f140 = (DataField) record.getVariableField("140");
				if(f105 != null)
				{
					Subfield f105a = ((DataField) record.getVariableField("105")).getSubfield('a');
					if(f105a != null)
					{
						String f105a11 = f105a.getData().substring(11, 12);
						log.info(bid + ": " + "(tipo " + type + ") trovato 105$a/11 = " + f105a11);
						if(f105a11.equals("i"))
						{
							{
								if(type == 'a')
									aSet.addAll(getNames(record, bid));
								else
									bSet.addAll(getNames(record, bid));
							}
						}
					}
				}
				else if(f140 != null)
				{
					log.info(bid + ": " + "(tipo " + type + ") trovato 140");
					Subfield f140a = ((DataField) record.getVariableField("140")).getSubfield('a');
					if(f140a != null)
					{
						String f140a1718 = f140a.getData().substring(17, 19);
						log.info(bid + ": " + "(tipo " + type + ") trovato 140$a/17-18 = " + f140a1718);
						if(f140a1718.equals("da"))
						{
							{
								if(type == 'a')
									aSet.addAll(getNames(record, bid));
								else
									bSet.addAll(getNames(record, bid));
							}
						}
					}
				}
			}

/*
 * Nel caso type = 'c' testiamo due date in 100$a, dopo averle ripulite
 */

			if(type == 'c')
			{
				data = ((DataField) record.getVariableField("100")).getSubfield('a').getData();
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
						cSet.addAll(getNames(record, bid));
					}
				}
			}
/*
 * Nel caso type = 'b' operiamo come per 'a', ma con un set separato
 */
			if(type == 'd')
			{
				dSet.addAll(getNames(record, bid));
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
	}

/*
 * Mette in un array l'eventuale unico file selezionato, oppure tutti i file
 * trovati nella directory selezionata, di tipo mrc.gz o mrc non compressi.
 */
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
					if(name.endsWith("mrc.gz") || name.endsWith("mrc"))
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
		TreeSet<String> tree = new TreeSet<String>(new Comp());
		StopWatch sw = new StopWatch();
		sw.start();
		int count = 0;
		for(File file : files)
		{
			console.info("Elaborazione file " + file.getName() + " (" + ++count + "/" + files.length + ")");
			tu.extract(file);
			tree.addAll(aSet);
			tree.addAll(bSet);
			tree.addAll(cSet);
			tree.addAll(dSet);
			sw.split();
			console.info(sw.toSplitString() + " (" + tree.size() + " nomi trovati finora)");
			sw.unsplit();
		}

		tu.output(tree, "nomi.csv");
		tu.output(aSet, "nomi-a.csv");
		tu.output(bSet, "nomi-b.csv");
		tu.output(cSet, "nomi-c.csv");
		tu.output(dSet, "nomi-d.csv");
		console.info("Tempo impiegato: " + sw.toString());
	}
}
