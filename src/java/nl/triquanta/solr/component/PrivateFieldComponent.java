package nl.triquanta.solr.component;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexReader;
import org.apache.commons.lang.StringUtils;

public class PrivateFieldComponent extends SearchComponent {
  
  @SuppressWarnings("unchecked")
  protected NamedList initParams;
  
  private static String ALLOWEDPARAM = "musallowedlevel"; 
  
  @Override
  @SuppressWarnings("unchecked")
  public void init(NamedList args) {
    super.init(args);
    this.initParams = args;
  }
  
  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    
  }
  
  @Override
  public void process(ResponseBuilder rb) throws IOException {
    Set<String> returnFields = getReturnFields(rb);
    DocSlice slice = (DocSlice) rb.rsp.getValues().get("response");
    SolrIndexReader reader = rb.req.getSearcher().getReader();
    SolrDocumentList rl = new SolrDocumentList();
    for (DocIterator it = slice.iterator(); it.hasNext();) {
      int docId = it.nextDoc();
      Document doc = reader.document(docId);
      // Determine the fields which may be shown
      List<Fieldable> fields = getPrintableFields(doc, rb);
      SolrDocument sdoc = new SolrDocument();
      // for each field, see if it has "_for_group_x" siblings, accumulate if so
      // but only if permitted
      for (Fieldable field : fields) {
        String fn = field.name();
        String target_fn = fn;
        if (returnFields.contains(target_fn)) {
          for (String v : doc.getValues(fn)) {
            if (sdoc.getFieldValues(target_fn) == null || !sdoc.getFieldValues(target_fn).contains(v)) {
              // "If fields already exist with this name it will append the collection" - http://lucene.apache.org/solr/api/org/apache/solr/common/SolrDocument.html
              sdoc.addField(target_fn, v);
            }
          }
        }
      } // for fields
      if (returnFields.contains("score")) {
        Float score = it.score();
        sdoc.setField("score", score);
      }

      rl.add(sdoc);
    }
    rl.setMaxScore(slice.maxScore());
    rl.setNumFound(slice.matches());
    rb.rsp.getValues().remove("response");
    rb.rsp.add("response", rl);
    
  }
  

  /**
   * Get the fields that may be shown to the user
   * @param doc
   * @return
   */
  private List<Fieldable> getPrintableFields(Document doc, ResponseBuilder rb) {
    List<Fieldable> fields = doc.getFields();
    Set<String> allowedFields = getAllowedFields();
    List<Fieldable> printableFields = new ArrayList<Fieldable>();
    // Determine if the document is private
    boolean isPrivate = documentIsPrivate(doc, rb);
    // Loop through the fields
    for (Fieldable field : fields) {
      // If the document is private, only allowed fields may be printed
      if (isPrivate) {
        String fieldname = field.name();
        if (allowedFields.contains(fieldname)) {
          printableFields.add(field);
        }
      }
      // Else all other fields may be printed as well.
      else {
        printableFields.add(field);
      }
    }
    return printableFields;
  }

  /**
   * Determine if a document is regarded private for a user.
   * @param doc
   * @return boolean isPrivate
   */
  private boolean documentIsPrivate(Document doc, ResponseBuilder rb) {
    boolean isPrivate = true;
    // Get the private field data
    String privateFieldname = getPrivateFieldName();
    List<String> publicValues = getAllowedValues(rb);
    // Get the values from the doc
    for (String v : doc.getValues(privateFieldname)) {
      if (publicValues.contains(v)) {
        isPrivate = false;
      }
    }
    return isPrivate;
  }

  private Set<String> getReturnFields(ResponseBuilder rb) {
    Set<String> fields = new HashSet<String>();
    String flp = rb.req.getParams().get(CommonParams.FL);
    if (StringUtils.isEmpty(flp)) {
      // called on startup with a null ResponseBuilder, so
      // we want to prevent a spurious NPE in the logs...
      return fields; 
    }
    String[] fls = StringUtils.split(flp, ",");
    IndexSchema schema = rb.req.getSchema();
    for (String fl : fls) {
      if ("*".equals(fl)) {
        Map<String,SchemaField> fm = schema.getFields();
        for (String fieldname : fm.keySet()) {
          SchemaField sf = fm.get(fieldname);
          if (sf.stored() && (! "content".equals(fieldname))) {
            fields.add(fieldname);
          }
        }
      } else if ("id".equals(fl)) {
        SchemaField usf = schema.getUniqueKeyField();
        fields.add(usf.getName());
      } else {
        fields.add(fl);
      }
    }
    return fields;
  }
  
  /**
   * Get the allowed fields
   * @return
   */
  private Set<String> getAllowedFields() {
    String[] allowed;
    Set<String> fields = new HashSet<String>();
    String temp = (String) this.initParams.get("allowedFields");
    allowed = temp.split(",");
    for (String field : allowed) {
      fields.add(field);
    }      
    return fields;
  }
  
  /**
   * Get the privateData field
   * @return
   */
  private String getPrivateFieldName() {
    return (String) this.initParams.get("privateField");
  }
  
  /**
   * Get the values for which the document is regarded as public
   * @return
   */
  private List<String> getAllowedValues(ResponseBuilder rb) {
    ArrayList<String> allowedValues = new ArrayList<String>();
    allowedValues.add((String) this.initParams.get("publicValue"));
    // Get the values from the request.
    String allowedParam = rb.req.getParams().get(PrivateFieldComponent.ALLOWEDPARAM);
    if (allowedParam != null) {
      String[] allowed;
      allowed = allowedParam.split(",");
      for (String value : allowed) {
        allowedValues.add(value);
      }
    }
    return allowedValues;
  }
  
  @Override
  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public String getSourceId() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public String getSource() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public String getVersion() {
    // TODO Auto-generated method stub
    return null;
  }
  
}
