import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class Fix
{
	private static String fixNonSort(String data)
	{
		data = data.replace("\u00c2\u0098", "\u00c2\u0088");
		data = data.replace("\u00c2\u009c", "\u00c2\u0089");
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
		while(reader.hasNext())
		{
			Record record = reader.next();
			List<DataField> fields = record.getDataFields();

// Scandisce la lista dei data field			
			
			Iterator<DataField> i = fields.iterator();
			while(i.hasNext())
			{
				DataField field = (DataField) i.next();
				if(true)
				{
					List<Subfield> subFields = field.getSubfields();
					Iterator<Subfield> j = subFields.iterator();
					while(j.hasNext())
					{
						Subfield subfield = (Subfield) j.next();
						String data = subfield.getData();
						data = Fix.fixNonSort(data);
						subfield.setData(data);
					}
				}
			}
			System.out.println(record.toString());
			writer.write(record);
		}
	}
}
