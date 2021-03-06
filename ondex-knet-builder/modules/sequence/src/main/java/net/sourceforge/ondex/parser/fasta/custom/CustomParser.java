package net.sourceforge.ondex.parser.fasta.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.ONDEXPluginArguments;
import net.sourceforge.ondex.args.FileArgumentDefinition;
import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.DataSource;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.event.ONDEXEventHandler;
import net.sourceforge.ondex.event.type.AttributeNameMissingEvent;
import net.sourceforge.ondex.event.type.ConceptClassMissingEvent;
import net.sourceforge.ondex.event.type.DataSourceMissingEvent;
import net.sourceforge.ondex.event.type.EventType.Level;
import net.sourceforge.ondex.event.type.GeneralOutputEvent;
import net.sourceforge.ondex.parser.fasta.FastaBlock;
import net.sourceforge.ondex.parser.fasta.Parser;
import net.sourceforge.ondex.parser.fasta.ReadFastaFiles;
import net.sourceforge.ondex.parser.fasta.WriteFastaFile;
import net.sourceforge.ondex.parser.fasta.args.ArgumentNames;

public class CustomParser {


    private ONDEXPluginArguments pa;

    private ConceptClass ccGene;
    private ConceptClass ccProt;
    private EvidenceType etIMPD;
    private AttributeName taxIdAttr;
    private AttributeName naAttr;
    private AttributeName aaAttr;
    private DataSource dataSourceUnknown;
    private String separator = " ";
    private Map<Integer, DataSource> accessions = new HashMap<Integer, DataSource>();

    public CustomParser(ONDEXPluginArguments pa) {
        this.pa = pa;
    }

    public void setONDEXGraph(ONDEXGraph graph) throws Exception {

        if (ccGene == null) {
            ccGene = graph.getMetaData().getConceptClass(MetaData.gene);
        }

        if (ccProt == null) {
            ccProt = graph.getMetaData().getConceptClass(MetaData.protein);
        }

        if (dataSourceUnknown == null) {
            dataSourceUnknown = graph.getMetaData().getDataSource(MetaData.unknown);
        }

        if (etIMPD == null) {
            etIMPD = graph.getMetaData().getEvidenceType(MetaData.IMPD);
        }

        if (taxIdAttr == null) {
            taxIdAttr = graph.getMetaData().getAttributeName(MetaData.taxID);
        }

        if (naAttr == null) {
            naAttr = graph.getMetaData().getAttributeName(MetaData.nucleicAcid);
        }

        if (aaAttr == null) {
            aaAttr = graph.getMetaData().getAttributeName(MetaData.aminoAcid);
        }

        List<String> accMap = (List<String>) pa.getObjectValueList(ArgumentNames.TO_ACC_ARG);
        if (accMap != null) {
            for (String str : accMap) {
                Integer key = Integer.valueOf(str.split(":")[0].trim());
                DataSource value = graph.getMetaData().getDataSource(str.split(":")[1].trim());
                accessions.put(key, value);
            }
        }

        GeneralOutputEvent so = new GeneralOutputEvent("Starting Simple Fasta File parsing...", Parser.getCurrentMethodName());
        so.setLog4jLevel(Level.INFO);
        ONDEXEventHandler.getEventHandlerForSID(graph.getSID()).fireEventOccurred(so);

        if (pa.getUniqueValue(ArgumentNames.SEPARATOR_ARG) != null)
            separator = (String) pa.getUniqueValue(ArgumentNames.SEPARATOR_ARG);

        List<String> fileList = (List<String>) pa.getObjectValueList(FileArgumentDefinition.INPUT_FILE);

        for (String fileName : fileList) {
            WriteFastaFile writeFastaFileSimple = new WriteFastaFileSimple();
            ReadFastaFiles.parseFastaFile(graph, fileName, writeFastaFileSimple);
        }

        so = new GeneralOutputEvent("Finished Simple Fasta File parsing...", Parser.getCurrentMethodName());
        so.setLog4jLevel(Level.INFO);
        ONDEXEventHandler.getEventHandlerForSID(graph.getSID()).fireEventOccurred(so);
    }

    //TODO: some DNA sequences may contain some N for an arbitry nucleotide
    private Pattern patternNotNA = Pattern.compile("[^A|T|G|C|U|a|t|g|c|u]");

    private class WriteFastaFileSimple extends WriteFastaFile {

        @Override
        public void parseFastaBlock(ONDEXGraph graph, FastaBlock fasta) throws InvalidPluginArgumentException {

            // digest header
            String header = fasta.getHeader().trim();
            String firstAccession = header;
            String desc = header;
            if (header.indexOf(separator) > 0) {
                firstAccession = header.substring(0, header.indexOf(separator)).trim();
                desc = header.substring(header.indexOf(separator) + 1, header.length()).trim();
            }

            String[] headerInfo = header.split(separator);

            String sequence = fasta.getSequence();

            boolean isNotNA = patternNotNA.matcher(sequence).find();

            ConceptClass cc;
            DataSource dataSource;
            AttributeName seqAt;
            if (isNotNA) {
                cc = ccProt;
                dataSource = dataSourceUnknown;
                seqAt = aaAttr;
            } else {
                cc = ccGene;
                dataSource = dataSourceUnknown;
                seqAt = naAttr;
            }

            //overwrite SeqType (seqAt) if parameter SeqType is set and valid
            String attr_name = (String) pa.getUniqueValue(ArgumentNames.TYPE_OF_SEQ_ARG);
            if (attr_name != null) {
                if (graph.getMetaData().getAttributeName(attr_name) != null) {
                    seqAt = graph.getMetaData().getAttributeName(attr_name);
                } else {
                    AttributeNameMissingEvent ge = new AttributeNameMissingEvent("Missing: " + attr_name, Parser.getCurrentMethodName());
                    ONDEXEventHandler.getEventHandlerForSID(graph.getSID()).fireEventOccurred(ge);
                }
            }

            //overwrite CC if parameter CC is set and valid
            String cc_name = (String) pa.getUniqueValue(ArgumentNames.CC_OF_SEQ_ARG);
            if (cc_name != null) {
                if (graph.getMetaData().getConceptClass(cc_name) != null) {
                    cc = graph.getMetaData().getConceptClass(cc_name);
                } else {
                    ConceptClassMissingEvent ge = new ConceptClassMissingEvent("Missing: " + cc_name, Parser.getCurrentMethodName());
                    ONDEXEventHandler.getEventHandlerForSID(graph.getSID()).fireEventOccurred(ge);
                }
            }

            //overwrite DataSource if parameter DataSource is set and valid
            String cv_name = (String) pa.getUniqueValue(ArgumentNames.CV_OF_SEQ_ARG);
            if (cv_name != null) {
                if (graph.getMetaData().getDataSource(cv_name) != null) {
                    dataSource = graph.getMetaData().getDataSource(cv_name);
                } else {
                    DataSourceMissingEvent ge = new DataSourceMissingEvent("Missing: " + cv_name, Parser.getCurrentMethodName());
                    ONDEXEventHandler.getEventHandlerForSID(graph.getSID()).fireEventOccurred(ge);
                }
            }

            ONDEXConcept ac = graph.getFactory().createConcept(firstAccession, "", dataSource, cc, etIMPD);
            ac.createConceptName(firstAccession, true);
            if (headerInfo != null) {
                for (int i = 0; i < headerInfo.length; i++) {
                    if (accessions.containsKey(i)) {
                        ac.createConceptAccession(headerInfo[i].trim(), accessions.get(i), false);
                    } else {
                        ac.createConceptName(headerInfo[i].trim(), false);
                    }
                }
            }
            ac.setDescription(desc);
            ac.createAttribute(taxIdAttr, pa.getUniqueValue(ArgumentNames.TAXID_TO_USE_ARG), true);
            ac.createAttribute(seqAt, sequence, false);
        }
    }
}
