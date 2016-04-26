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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.samples.RemoveLocalFieldsExample;

/**
 * Removes local field (tag 9XX).
 * 
 * @author Bas Peters
 */
public class Remove899Example
{

	public static void main(String args[]) throws Exception
	{

// InputStream input = RemoveLocalFieldsExample.class
// .getResourceAsStream("resources/edit.mrc");
//		InputStream input = RemoveLocalFieldsExample.class.getResourceAsStream(args[0]);
		InputStream input = new FileInputStream(args[0]);

		String ext = FilenameUtils.getExtension(args[0]);
		String noext = FilenameUtils.removeExtension(args[0]);

		FileOutputStream output = new FileOutputStream(noext + "-strip." + ext);

		MarcReader reader = new MarcStreamReader(input);
		MarcStreamWriter writer = new MarcStreamWriter(output);
		while(reader.hasNext())
		{
			Record record = reader.next();
			// System.out.println(record.toString());

			Pattern f899 = Pattern.compile("899");
			Pattern f921 = Pattern.compile("921");

			List fields = record.getDataFields();

			Iterator i = fields.iterator();
			while(i.hasNext())
			{
				DataField field = (DataField) i.next();
				Matcher matcher = f899.matcher(field.getTag());
				if(matcher.matches()) i.remove();
				List subFields = field.getSubfields();
				Matcher m921 = f921.matcher(field.getTag());
				if(m921.matches())
				{
					Iterator j = subFields.iterator();
					while(j.hasNext())
					{
						Subfield subfield = (Subfield) j.next();
						switch(subfield.getCode())
						{
							case 'a':
								break;
							case 'b':
								break;
							case 'c':
								break;
							default:
								System.err.println("Rimuovo " + subfield.getCode() + " (" + subfield.getData() + ")");
								j.remove();
								break;
						}
					}
				}
			}
			System.out.println(record.toString());
			writer.write(record);
		}
	}

}
