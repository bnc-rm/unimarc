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
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import javax.swing.JViewport;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

/**
 * Removes local field (tag 9XX).
 * 
 * @author Bas Peters
 */
public class Export
{

	private static int max = 100000; // 537082;

	public static void main(String args[]) throws Exception
	{

		InputStream input = new FileInputStream(args[0]);

		String ext = FilenameUtils.getExtension(args[0]);
		String noext = FilenameUtils.removeExtension(args[0]);

		Writer output = new OutputStreamWriter(new FileOutputStream(noext + ".json"), "ISO-8859-1");

		MarcReader reader = new MarcStreamReader(input);
		JSONArray jRecords = new JSONArray();
		int count = 0;
		while(reader.hasNext())
		{
			Record record = reader.next();
			String bid = null;
			JSONObject jRecord = new JSONObject();

			char status = record.getLeader().toString().charAt(5);
			System.out.println("stato: " + status);

			char level = record.getLeader().toString().charAt(7);
			System.out.println("livello: " + level);
			switch(level)
			{
				case 'm':
					jRecord.put("livello", "monografia");
					break;
				case 'a':
					jRecord.put("livello", "spoglio");
					break;
				case 's':
					jRecord.put("livello", "periodico");
					break;
				default:
					break;
			}

			char tipo = record.getLeader().toString().charAt(6);
			switch(tipo)
			{
				case 'a':
					jRecord.put("tipo", "Testo a stampa");
					break;
				case 'b':
					jRecord.put("tipo", "Testo manoscritto");
					break;
				case 'c':
					jRecord.put("tipo", "Musica a stampa");
					break;
				case 'd':
					jRecord.put("tipo", "Musica manoscritta");
					break;
				case 'e':
					jRecord.put("tipo", "Cartografia a stampa");
					break;
				case 'f':
					jRecord.put("tipo", "Cartografia manoscritta");
					break;
				case 'g':
					jRecord.put("tipo", "Materiale video");
					break;
				case 'k':
					jRecord.put("tipo", "Materiale grafico");
					break;
				case 'i':
					jRecord.put("tipo", "Registrazione sonora non musicale");
					break;
				case 'j':
					jRecord.put("tipo", "Registrazione sonora musicale");
					break;
				case 'l':
					jRecord.put("tipo", "Risorsa elettronica");
					break;
				case 'm':
					jRecord.put("tipo", "Materiale multimediale");
					break;
				case 'r':
					jRecord.put("tipo", "Oggetto multimediale");
					break;
				default:
					break;
			}

			List<DataField> dFields = record.getDataFields();
			List<ControlField> cFields = record.getControlFields();

// Scandisce la lista dei data field

			Iterator<DataField> dfIter = dFields.iterator();
			Iterator<ControlField> cfIter = cFields.iterator();
			while(cfIter.hasNext())
			{
				ControlField field = (ControlField) cfIter.next();
				String tag = field.getTag();
				switch(tag)
				{
					case "001":
// bid = field.getData().substring(8, 11);
// bid += "\\" + field.getData().substring(12);
						bid = field.getData();
						jRecord.put("codiceIdentificativo", bid);
						System.out.println("codiceIdentificativo: " + bid);
						break;
					default:
						break;
				}
			}
// if(!bid.contains("BVE") && (bid.compareToIgnoreCase("IT\\ICCU\\BVE\\0655516")
// < 0))
			if(bid.compareToIgnoreCase("IT\\ICCU\\BVE\\0655516") < 0)
// if(bid.compareToIgnoreCase("IT\\ICCU\\BVE\\0037416") < 0)
			{
				continue;
			}
			System.out.println(++count);
			JSONArray jLocs = new JSONArray();
			JSONArray jNomi = new JSONArray();
			JSONArray jCDDs = new JSONArray();
			JSONArray jNumeri = new JSONArray();
			JSONArray jSoggetti = new JSONArray();
			JSONArray jVarianti = new JSONArray();
			JSONObject jNumero = null;
			JSONObject jCDD = null;
			while(dfIter.hasNext())
			{
				DataField field = (DataField) dfIter.next();
				String tag = field.getTag();
				String data = null;
				switch(tag)
				{
					case "010":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
						}
						else
						{
							data = field.getSubfield('z').getData() + " (" + field.getSubfield('b').getData() + ")";
						}
						jNumero = new JSONObject();
						jNumero.put("ISBN", data);
						jNumeri.add(jNumero);
						break;
					case "011":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
						}
						else
						{
							data = field.getSubfield('z').getData() + " (" + field.getSubfield('b').getData() + ")";
						}
						jNumero = new JSONObject();
						jNumero.put("ISSN", data);
						jNumeri.add(jNumero);
						break;
					case "100":
						data = field.getSubfield('a').getData();
						System.out.println("charset: " + data.substring(26, 29));
						break;
					case "101":
						data = field.getSubfield('a').getData();
						jRecord.put("linguaPubblicazione", data);
						break;
					case "102":
						data = field.getSubfield('a').getData();
						jRecord.put("paesePubblicazione", data);
						break;
					case "200":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
							data = data.replace("\u00c2\u0089", "");
							data = data.replace("\u00c2\u0088", "");
						}
						else
						{
							System.err.println(bid + ": privo di titolo");
						}
						if(field.getSubfield('f') != null)
						{
							data += " / " + field.getSubfield('f').getData();
						}
						jRecord.put("titolo", data);
						System.out.println("titolo: " + data);
						break;
					case "210":
						data = "";
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
						}
						if(field.getSubfield('c') != null)
						{
							data += " : " + field.getSubfield('c').getData();
						}
						if(field.getSubfield('d') != null)
						{
							data += ", " + field.getSubfield('d').getData();
						}
						jRecord.put("pubblicazione", data);
						break;
					case "215":
						data = "";
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
						}
						if(field.getSubfield('d') != null)
						{
							data += " ; " + field.getSubfield('d').getData();
						}
						jRecord.put("descrizioneFisica", data);
						break;
					case "410":
						data = "";
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
						}
						if(field.getSubfield('v') != null)
						{
							data += " ; " + field.getSubfield('v').getData();
						}
						data = data.replace("\u00c2\u0089", "");
						data = data.replace("\u00c2\u0088", "");
						jRecord.put("collezione", data);
						break;
					case "461":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
							data = data.replace("\u00c2\u0089", "");
							data = data.replace("\u00c2\u0088", "");
						}
						else
						{
							System.err.println(bid + ": privo di titolo");
						}
						if(field.getSubfield('f') != null)
						{
							data += " / " + field.getSubfield('f').getData();
						}
						jRecord.put("contenutoIn", data);
						break;
					case "517":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
							data = data.replace("\u00c2\u0089", "");
							data = data.replace("\u00c2\u0088", "");
						}
						else
						{
							System.err.println(bid + ": privo di titolo");
						}
						jVarianti.add(data);
						break;
					case "606":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
						}
						if(field.getSubfields('x') != null)
						{
							Iterator iter = field.getSubfields('x').iterator();
							if(iter.hasNext())
							{
								data += " - " + ((Subfield) iter.next()).getData();
								while(iter.hasNext())
								{
									data += " - " + ((Subfield) iter.next()).getData();
								}
							}
						}
						jSoggetti.add(data);
// jRecord.put("soggetti", data);
						break;
					case "676":
						if(field.getSubfield('a') != null)
						{
							data = field.getSubfield('a').getData();
							jCDD = new JSONObject();
							jCDD.put("cdd", data);
						}
						data = field.getSubfield('c').getData();
						jCDD.put("dec", data);
						data = field.getSubfield('v').getData();
						jCDD.put("ed", data);
						jCDDs.add(jCDD);
						break;
					case "700":
						data = field.getSubfield('a').getData();
						if(field.getSubfield('b') != null)
						{
							data += field.getSubfield('b').getData();
						}
						jNomi.add(data);
						jRecord.put("autorePrincipale", data);
						break;
					case "701":
						data = field.getSubfield('a').getData();
						if(field.getSubfield('b') != null)
						{
							data += field.getSubfield('b').getData();
						}
						jNomi.add(data);
						break;
					case "711":
						data = field.getSubfield('a').getData();
						if(field.getSubfield('b') != null)
						{
							data += field.getSubfield('b').getData();
						}
						jNomi.add(data);
						break;
					case "710":
						data = field.getSubfield('a').getData();
						if(field.getSubfield('b') != null)
						{
							data += field.getSubfield('b').getData();
						}
						jRecord.put("autorePrincipale", data);
						break;
					case "702":
						data = field.getSubfield('a').getData();
						if(field.getSubfield('b') != null)
						{
							data += field.getSubfield('b').getData();
						}
						jNomi.add(data);
						break;
					case "712":
						data = field.getSubfield('a').getData();
						if(field.getSubfield('b') != null)
						{
							data += field.getSubfield('b').getData();
						}
						jNomi.add(data);
						break;
					case "899":
						data = field.getSubfield('1').getData();
						JSONObject jLoc = new JSONObject();
						jLoc.put("isil", data);
						jLocs.add(jLoc);
						break;
					default:
						break;
				}
			}
			if(!bid.contains("ZZZ"))
			{
				jRecord.put("localizzazioni", jLocs);
				if(jNomi.size() > 0) jRecord.put("nomi", jNomi);
				if(jNumeri.size() > 0) jRecord.put("numeri", jNumeri);
				if(jCDDs.size() > 0) jRecord.put("dewey", jCDDs);
				if(jSoggetti.size() > 0) jRecord.put("soggetti", jSoggetti);
				if(jVarianti.size() > 0) jRecord.put("altriTitoli", jVarianti);
				jRecords.add(jRecord);
			}
			if(count >= max) break;
		}
// output.write(jRecords.toString());
// jRecords.writeJSONString(output);
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(jRecords.toString());
		String prettyJsonString = gson.toJson(je);
		output.write(prettyJsonString);
// output.write(jRecords.toString());
		output.flush();
		output.close();
	}
}
