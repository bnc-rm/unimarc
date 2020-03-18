
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

/**
 * Removes local field (tag 9XX).
 * 
 * @author Bas Peters
 */
public class ScivolamentoFondi950
{

	public static void main(String args[]) throws Exception
	{

		// InputStream input = RemoveLocalFieldsExample.class
		// .getResourceAsStream("resources/edit.mrc");
		// InputStream input =
		// RemoveLocalFieldsExample.class.getResourceAsStream(args[0]);
		InputStream input = new FileInputStream(args[0]);

		String ext = FilenameUtils.getExtension(args[0]);
		String noext = FilenameUtils.removeExtension(args[0]);

		FileOutputStream output = new FileOutputStream(noext + "-new." + ext);

		MarcReader reader = new MarcStreamReader(input);
		MarcStreamWriter writer = new MarcStreamWriter(output);
		int totalCount = 0;
		int fondsCount = 0;
		while(reader.hasNext())
		{
			Record record = reader.next();
			boolean hasFonds = false;

			char level = record.getLeader().getImplDefined1()[0];

/*
 * Consideriamo solo seriali, che sembrano individuati tramite due codici,
 * "s" e "c", almeno in UNIMARC. Poi si scorrono tutti i campi
 * intervenendo
 * solo sulla 950, in teoria unica.
 */

			if(level == 's' || level == 'c')
			{
				Pattern f950 = Pattern.compile("950");
				List<DataField> fields = record.getDataFields();
				Iterator<DataField> i = fields.iterator();
				while(i.hasNext())
				{
					DataField field = (DataField) i.next();
					List<Subfield> subFields = field.getSubfields();

// Perché usare Pattern e Matcher per individuare la 950? È solo una
// stringa.

					Matcher m950 = f950.matcher(field.getTag());
					LinkedBlockingQueue<Subfield> Fondi = new LinkedBlockingQueue<Subfield>();
					LinkedBlockingQueue<Subfield> NonFondi = new LinkedBlockingQueue<Subfield>();
					LinkedBlockingQueue<Subfield> temp = new LinkedBlockingQueue<Subfield>();
					if(m950.matches())
					{
						Iterator<Subfield> j = subFields.iterator();

// Il primo sottocampo dev'essere $a. Meglio esserne sicuri, per
// cui qui si effettua un controllo.

						if(j.next().getCode() != 'a')
						{
							System.err.println("La 950 non comincia con $a, proseguo");
						}

/*
 * Ogni sottocampo viene inserito in una Queue temporanea. Se si trova un $d
 * contenente "F.", si pone hasFonds = true e si continua a scorrere i
 * sottocampi. Trovato un $b o $c, o la fine della 950, la Queue viene vuotata
 * nel vettore Fondi o NonFondi a seconda che sia stato trovato un $d con "F.".
 */

						boolean bFound = false;
						boolean cFound = false;
						boolean isFirstB = true;
						boolean isFirstC = true;
						boolean isFirstColl = true;
						while(j.hasNext())
						{
							Subfield subfield = (Subfield) j.next();

/*
 * La difficoltà è soprattutto nel capire quando è finita una collocazione e ne
 * comincia un'altra. La presenza di $b (o un $c senza $b) indica sempre una
 * nuova collocazione, per cui va innanzitutto distinta la prima collocazione da
 * tutte le altre.
 */

							switch (subfield.getCode())
							{
								case 'b':
									if(!isFirstColl)
									{
										while(!temp.isEmpty())
										{
											if(hasFonds)
												Fondi.add(temp.remove());
											else
												NonFondi.add(temp.remove());
										}
									}
									else
									{
										isFirstB = false;
										isFirstColl = false;
									}
								case 'c':
									if(!isFirstColl)
									{
										while(!temp.isEmpty())
										{
											if(hasFonds)
												Fondi.add(temp.remove());
											else
												NonFondi.add(temp.remove());
										}
									}
									else
									{
										isFirstC = true;
										isFirstColl = false;
									}
								case 'd':
									if(subfield.getData().contains("CRF."))
									{
										System.out
										    .println("Fondo: $d = [" + subfield.getData() + "]");
										hasFonds = true;
									}
									break;
								default:
									break;
							}

/*
 * Fondi o meno, i sottocampi si aggiungo alla coda temporanea, ma nello stesso
 * tempo, esclsuo il $a, sono rimossi dalla 950, per essere aggiunti alla fine
 * in due gruppi distinti
 */

							temp.add(subfield);
							j.remove();
						}

/*
 * Ora la coda temporanea contiene l'ultima collocazione, come elenco di
 * sottocampi. Bisogna riversare anche questi nei due gruppi distinti,
 * altrimenti l'ultima collocazione va perduta.
 */

						while(!temp.isEmpty())
						{
							if(hasFonds)
								Fondi.add(temp.remove());
							else
								NonFondi.add(temp.remove());
						}

/*
 * Finito di scorrere la 950, in cui è rimasto solo il $a, si aggiungono di
 * nuovo tutti i sottocampi, ma prima i non fondi, poi quelli relativi a fondi.
 * Questa parte potrebbe non funzionare, se la lista dei sottocampi non si è
 * realmente svuotata intervenendo solo sull'iteratore.
 */

						while(!Fondi.isEmpty())
						{
							subFields.add(Fondi.remove());
						}
						while(!NonFondi.isEmpty())
						{
							subFields.add(NonFondi.remove());
						}
					}
				}
			}
			writer.write(record);
		}
	}
}
