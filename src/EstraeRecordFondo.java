
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
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

/**
 * Estrae da un file MARC solo i record contenenti "CRF." nella 950
 */
public class EstraeRecordFondo
{

  public static void main(String args[]) throws Exception
  {
    // InputStream input = RemoveLocalFieldsExample.class
    // .getResourceAsStream("resources/edit.mrc");
    // InputStream input =
    // RemoveLocalFieldsExample.class.getResourceAsStream(args[0]);
    InputStream input = new FileInputStream(args[0]);
    String owner = args[1];
    String sep = ";";
    String permaBase = "http://bve.opac.almavivaitalia.it/opac2/BVE/dettaglio/documento/";

    String ext = FilenameUtils.getExtension(args[0]);
    String noext = FilenameUtils.removeExtension(args[0]);

    FileOutputStream output = new FileOutputStream(noext + "-fondi." + ext);

    MarcReader reader = new MarcStreamReader(input, "UTF-8");
// MarcReader reader = new MarcStreamReader(input);
    MarcStreamWriter writer = new MarcStreamWriter(output);
    PrintWriter table = new PrintWriter(noext + ".csv", "UTF-8");
    int totalCount = 0;
    int fondsCount = 0;
    String csv = "autore;titolo;luogo;editore;anno;permalink;collocazioni\n";
    while(reader.hasNext())
    {
      Record record = reader.next();
      String permaLink = "";
      boolean hasFonds = false;
      List<ControlField> cFields = record.getControlFields();
      Iterator<ControlField> cfIter = cFields.iterator();
      while(cfIter.hasNext())
      {
        ControlField field = (ControlField) cfIter.next();
        String tag = field.getTag();
        switch(tag)
        {
        case "001":
          permaLink = permaBase + field.getData();
          break;
        default:
          break;
        }
      }

      List<DataField> fields = null;
      Iterator<DataField> i = null;

      char level = record.getLeader().getImplDefined1()[0];

      // Scartiamo ogni monografia

      if(level != 's' && level != 'c')
      {
        // System.out.println(level);
        Pattern f317 = Pattern.compile("317");

        fields = record.getDataFields();

        i = fields.iterator();
        while(i.hasNext())
        {
          DataField field = (DataField) i.next();
          List<Subfield> subFields = field.getSubfields();
          Matcher m317 = f317.matcher(field.getTag());
          if(m317.matches())
          {
            Iterator<Subfield> j = subFields.iterator();
            while(j.hasNext())
            {
              Subfield subfield = (Subfield) j.next();
              switch(subfield.getCode())
              {
              case 'a':
                if(subfield.getData().contains(owner))
                {
                  System.err
                      .println("$d = [" + subfield.getData() + "]");
                  hasFonds = true;
                }
                break;
              default:
                // System.out.println(
                // "$" + subfield.getCode() + " = " + subfield.getData());
                break;
              }
            }
          }
        }
      }
      if(hasFonds)
      {
        fondsCount++;
        writer.write(record);
        i = fields.iterator();
        String coll = "";
        boolean moreColl = false;
        while(i.hasNext())
        {
          DataField field = (DataField) i.next();
          String tag = field.getTag();
          if(tag.equals("200"))
          {
            System.err.println("200$f");
            if(field.getSubfield('f') != null)
              csv += field.getSubfield('f').getData() + sep; // autore
            else
              csv += sep;
            System.err.println("200$a");
            if(field.getSubfield('a') != null)
            {
              String temp = field.getSubfield('a').getData();
              temp = temp.replace("", "");
              temp = temp.replace("", "");
              csv += temp + sep; // titolo
            }
            else
              csv += sep;
          }
          if(tag.equals("210"))
          {
            System.err.println("210$a");
            if(field.getSubfield('a') != null)
              csv += field.getSubfield('a').getData() + sep; // luogo
            else
              csv += sep;
            System.err.println("210$b");
            if(field.getSubfield('c') != null)
              csv += field.getSubfield('c').getData() + sep; // editore
            else
              csv += sep;
            System.err.println("210$d");
            if(field.getSubfield('d') != null)
              csv += field.getSubfield('d').getData() + sep; // data
            else
              csv += sep;
          }
          if(tag.equals("950"))
          {
            System.err.println("950$d");
            if(field.getSubfield('a').getData().contains("Biblioteca Nazionale Centrale Roma"))
            {
              List<Subfield> sf = field.getSubfields('d');
              Iterator<Subfield> isf = sf.iterator();
              while(isf.hasNext())
              {
                String temp = isf.next().getData().trim();
                temp = temp.replaceAll("  ", "");
                if(moreColl)
                {
                  temp = "," + temp;
                }
                coll += temp; // collocazione
                moreColl = true;
              }
            }
          }
        }
        csv += permaLink + sep + coll + "\n";
      }
      System.err
          .println("Totale: " + ++totalCount + " (fondi: " + fondsCount + ")");
    }

    System.out.println(csv);
    table.print(csv);
    table.close();
    writer.close();
  }
}
