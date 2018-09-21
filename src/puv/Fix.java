package puv;

/*
 * Interventi vari su file scaricati dal polo PUV.
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

/*
 * Oltre ad altri tipici errori sui non-sort, PUV aggiunge l'uso di "<<" e ">>".
 */
public class Fix {
	private static String fixNonSort(String data) {
		data = data.replace("\u00c2\u0098", "\u00c2\u0088");
		data = data.replace("\u00c2\u009c", "\u00c2\u0089");
		data = data.replace("<<", "\u00c2\u0089");
		data = data.replace(">>", "\u00c2\u0089");
		return data;
	}

	private static String fixFillChar(String data) {
		data = data.replace("-", " ");
		return data;
	}

	public static void main(String args[]) throws Exception
	{
		InputStream input = new FileInputStream(args[0]);

		String ext = FilenameUtils.getExtension(args[0]);
		String noext = FilenameUtils.removeExtension(args[0]);

		FileOutputStream output = new FileOutputStream(noext + "-fix." + ext);

		MarcReader reader = new MarcStreamReader(input);
		MarcStreamWriter writer = new MarcStreamWriter(output);
		Record oRecord;
		while(reader.hasNext())
		{
			Record record = reader.next();
			if(record.getLeader().marshal().charAt(7) == 'm')
			{
			oRecord = MarcFactory.newInstance().newRecord();
			oRecord.setLeader(record.getLeader());
			
// PUV usa "-" invece di " " come fill nelle 100			
			
			List<ControlField> cFields = record.getControlFields();
			Iterator<ControlField> cfi = cFields.iterator();
			while(cfi.hasNext())
			{
				ControlField field = (ControlField) cfi.next();
				if(true)
				{
						String data = field.getData();
						data = Fix.fixFillChar(data);
						field.setData(data);
				}
				oRecord.getControlFields().add(field);
			}

			List<DataField> dFields = record.getDataFields();

// Scandisce la lista dei data field			
			
			Iterator<DataField> dfi = dFields.iterator();
			while(dfi.hasNext())
			{
				DataField field = (DataField) dfi.next();
				DataField oField = MarcFactory.newInstance().newDataField();
				String tag = field.getTag();
				if(tag.equals("Z30"))
				{
					tag = "950";
					field.setTag(tag);
				}
				if(true)
				{
					List<Subfield> subFields = field.getSubfields();
					Iterator<Subfield> j = subFields.iterator();
					while(j.hasNext())
					{
						Subfield subfield = (Subfield) j.next();
						String data = subfield.getData();
						data = Fix.fixNonSort(data);
						if(tag.startsWith("1"))
						{
							data = Fix.fixFillChar(data);
						}
						
/*
 * Lo Z30 prodotto da PUV può essere mappato sulla 950, ma il $e deve essere esattamente di 14 caratteri						
 */
						
						if(tag.equals("950") )
						{
							char code = subfield.getCode();
							switch (code) {
							case 'A':
								subfield.setCode('a');
								break;
							case '3':
								subfield.setCode('c');
								break;
							case '6':
								subfield.setCode('e');
								data = "00000" + data.substring(11);
								break;
							case '1':
								subfield.setCode('z');
								break;
							default:
//								field.removeSubfield(subfield);
								j.remove();
								break;
							}
							field.setTag("950");
							tag = "950";
							data = Fix.fixFillChar(data);
						}
						subfield.setData(data);
						oField.getSubfields().add(subfield);
					}
					
// Rimuove ogni tag non numerico, totalmente fuori standard						
					
					try
					{
						if(Integer.decode(tag) > 1000)
						{
							dfi.remove();
						}
						else
						{
							oField.setTag(tag);
							oField.setIndicator1(field.getIndicator1());
							oField.setIndicator2(field.getIndicator2());
							oRecord.getDataFields().add(oField);
						}
					}
					catch(NumberFormatException e)
					{
						dfi.remove();
					}
				}
			}
			System.out.println(oRecord.toString());
			writer.write(oRecord);
			}
		}
	}
}
