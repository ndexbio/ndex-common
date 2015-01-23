package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.common.util.TermStringType;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Network;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/*
 * Lines in the SIF file specify a source node, a relationship type
 * (or edge type), and one or more target nodes.
 * 
 * see: http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats
 */
public class ExcelParser implements IParsingEngine
{
    private final File excelFile;
    private final String excelURI;
    private final List<String> msgBuffer;
    private Network network;
    private String ownerName;
  //  private ExcelNetworkService networkService;
    private NdexPersistenceService networkService;


    public ExcelParser(String fileName, String ownerName, NdexDatabase db) throws Exception
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fileName), "A filename is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName), 
        		"A network owner name is required");
        this.setOwnerName(ownerName);
        this.networkService = new NdexPersistenceService(db);
        this.msgBuffer = Lists.newArrayList();
        this.excelFile = new File(fileName);
        this.excelURI = excelFile.toURI().toString();
    }
    
    public File getExcelFile()
    {
        return excelFile;
    }

    

    /**************************************************************************
    * The first worksheet is the data
    * 
    * If there is a second worksheet, it holds meta information
    * in a property-value format.
    **************************************************************************/
    @Override
	public void parseFile()
    {

        this.getMsgBuffer().add("Parsing lines from " + this.getExcelURI());
        // BufferedReader bufferedReader;
        FileInputStream excelFileStream;
        try
        {
            // bufferedReader = new BufferedReader(new
            // FileReader(this.getExcelFile()));
            excelFileStream = new FileInputStream(this.getExcelFile());
        }
        catch (FileNotFoundException e1)
        {
            this.getMsgBuffer().add("Could not read " + this.getExcelURI());
            this.networkService.abortTransaction();
            // e1.printStackTrace();
            return;
        }

        try
        {
           


            // Get the workbook instance for XLS file
            HSSFWorkbook workbook = new HSSFWorkbook(excelFileStream);

            int sheetCount = workbook.getNumberOfSheets();

            if (sheetCount == 0)
                throw new Exception("Empty Excel Workbook");

            // Get first sheet from the workbook - this is the data
            HSSFSheet sheet = workbook.getSheetAt(0);


            // Get second sheet from the workbook
            // If it exists, it is the metaData
            HSSFSheet metaDataSheet = null;
            ;
            if (sheetCount > 1)
            {
                metaDataSheet = workbook.getSheetAt(1);
            }

            createNetwork();

            if (null == metaDataSheet)
            {
                this.buildNetworkFromWorksheet(sheet);
            }
            else
            {
                this.buildNetworkFromWorkSheet(sheet, metaDataSheet);
            }

            // persist the network domain model, commit the transaction, close
            // database connection
			// set the source format
			this.networkService.setNetworkSourceFormat(NetworkSourceFormat.EXCEL);

            this.networkService.persistNetwork();
        }
        catch (Exception e)
        {
            // rollback current transaction and close the database connection
            this.networkService.abortTransaction();
            e.printStackTrace();
        } 
    }
  
    private void createNetwork() throws Exception
    {
        String networkTitle = this.excelFile.getName();
        this.networkService.createNewNetwork(ownerName, networkTitle, null);
        		
        //this.networkService.setFormat("NDEX_EXCEL");
        this.getMsgBuffer().add("New Excel: " + network.getName());
    }


    private Long addNode(String name) throws ExecutionException, NdexException
    {
       if ( name == null )
    	   throw new NdexException ("empty name found when creating node.");
    	TermStringType stype = TermUtilities.getTermType(name);
		if ( stype == TermStringType.NAME) {
			return networkService.getNodeIdByName(name);
		} 
		return networkService.getNodeIdByBaseTerm(name);
    }

    
    private void addEdge(String subject, String predicate, String object) throws ExecutionException, NdexException
    {
        Long subjectNode = addNode(subject);
        Long objectNode = addNode(object);
        Long predicateTerm = this.networkService.getBaseTermId(predicate);
        this.networkService.getEdge(subjectNode, objectNode, predicateTerm, null, null, null);
    }

    /*
    	public ValidationState getValidationState() {
    		return this.validationState;
    	}
    */
    public List<String> getMsgBuffer()
    {
        return this.msgBuffer;
    }

    public String getExcelURI()
    {
        return excelURI;
    }

    private void buildNetworkFromWorksheet(HSSFSheet sheet) throws ExecutionException, NdexException
    {
        processDataSheet(sheet);
    }

    private void buildNetworkFromWorkSheet(HSSFSheet dataSheet, HSSFSheet metaDataSheet) throws ExecutionException, NdexException
    {
        // Iterate over the first 100(?) rows looking for values in col 0
        // Those will be the properties
        // Values in col 1 will be values
        // We will take the properties that we know and comment on those that
        // are
        // not known. Rows with cell 0 blank are ignored.
        Iterator<Row> rowIterator = metaDataSheet.iterator();

        // TODO: use metaDataSheet

        processDataSheet(dataSheet);

    }

    private void processDataSheet(HSSFSheet sheet) throws ExecutionException, NdexException
    {
        Iterator<Row> rowIterator = sheet.iterator();

        // The first row should have the headers, skip them.
        Row headerRow = rowIterator.next();
        // We are looking for the column names "source", "target", "relation"

        // TODO add support for other columns

        // Iterate over the remaining rows to load each edge
        while (rowIterator.hasNext())
        {
            Row row = rowIterator.next();
            String subjectIdentifier = getCellText(row.getCell(0));
            String predicateIdentifier = getCellText(row.getCell(1));
            String objectIdentifier = getCellText(row.getCell(2));

            if (!subjectIdentifier.isEmpty() && !predicateIdentifier.isEmpty() && !objectIdentifier.isEmpty())
            {
                addEdge(subjectIdentifier, predicateIdentifier, objectIdentifier);
            }
            else if (!subjectIdentifier.isEmpty())
            {
                addNode(subjectIdentifier);
            }

        }
    }


    private String getCellText(Cell cell)
    {
        switch (cell.getCellType())
        {
            case Cell.CELL_TYPE_BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_NUMERIC:
                return Double.toString(cell.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
        }
        return "";
    }



	public String getOwnerName() {
		return ownerName;
	}



	private void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	
	@Override
	public UUID getUUIDOfUploadedNetwork() {
		try { 
			return networkService.getCurrentNetwork().getExternalId();
		} catch ( Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
