package net.moraleboost.flux.eval.stmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import net.moraleboost.flux.eval.EvalContext;
import net.moraleboost.flux.eval.EvalException;
import net.moraleboost.flux.eval.Expression;
import net.moraleboost.flux.eval.Namespace;

public class SelectStatement extends BaseStatement
{
    public static enum SortOrder
    {
        Ascending, Descending
    }
    
    public static class Sort
    {
        public String field;
        public SortOrder order;
    }
    
    public static class ResultField
    {
        public Expression expression;
        public String alias;
    }
    
    private String source;
    private List<ResultField> resultFields;
    private Expression condition;
    private String nativeQuery;
    private List<Sort> sortConditions;
    private Integer limit;
    private Integer offset;
    
    public SelectStatement()
    {
        super();
    }
    
    public void setSource(String source)
    {
        this.source = source;
    }
    
    public String getSource()
    {
        return source;
    }
    
    public void addResultField(ResultField resultField)
    {
        resultFields.add(resultField);
    }
    
    public void setResultFields(List<ResultField> resultFields)
    {
        this.resultFields = resultFields;
    }
    
    public List<ResultField> getResultFields()
    {
        return resultFields;
    }
    
    public void setCondition(Expression expr)
    {
        this.condition = expr;
    }
    
    public Expression getCondition()
    {
        return condition;
    }
    
    public void setNativeQuery(String query)
    {
        this.nativeQuery = query;
    }
    
    public String getNativeQuery()
    {
        return nativeQuery;
    }
    
    public void addSortCondition(Sort sortCondition)
    {
        if (sortConditions == null) {
            sortConditions = new ArrayList<Sort>();
        }
        sortConditions.add(sortCondition);
    }

    public void setSortConditions(List<Sort> sortConditions)
    {
        this.sortConditions = sortConditions;
    }

    public List<Sort> getSortConditions()
    {
        return sortConditions;
    }

    public void setLimit(Integer limit)
    {
        this.limit = limit;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public void setOffset(Integer offset)
    {
        this.offset = offset;
    }

    public Integer getOffset()
    {
        return offset;
    }

    @SuppressWarnings("unchecked")
    public Result execute(EvalContext ctx) throws EvalException
    {
        SolrServer server = getSolrServer(source, ctx);
        
        SolrQuery query = new SolrQuery();
        // retrieve all fields + "score" by default.
        query.setIncludeScore(true);
        
        if (condition != null) {
            query.setQuery(condition.toSolrQuery(ctx));
        } else if (nativeQuery != null) {
            query.setQuery(nativeQuery);
        } else {
            query.setQuery("*:*");
        }
        
        if (limit != null) {
            query.setRows(limit);
        }
        if (offset != null) {
            query.setStart(offset);
        }
        
        if (sortConditions != null) {
            for (Sort s: sortConditions) {
                if (s.order == SortOrder.Ascending) {
                    query.addSortField(s.field, SolrQuery.ORDER.asc);
                } else if (s.order == SortOrder.Descending) {
                    query.addSortField(s.field, SolrQuery.ORDER.desc);
                } else {
                    throw new EvalException("Unknown sort order.");
                }
            }
        }
        
        try {
            ctx.enterFunction();
            Namespace ns = ctx.getNamespace();            
            QueryResponse resp = server.query(query, SolrRequest.METHOD.POST);
//            QueryResponse resp = server.query(query);
            Result result = new Result(query.getQuery(), resp);
            
            for (SolrDocument doc: resp.getResults()) {            
                ns.setCurrent(doc);

                SolrDocument resdoc = new SolrDocument();
                for (ResultField rf: resultFields) {
                    Object o = rf.expression.evaluate(ctx);
                    if (o instanceof Map) {
                        resdoc.putAll((Map<? extends String, ? extends Object>)o);
                    } else {
                        resdoc.put(rf.alias, o);
                    }
                }
                result.addDocument(resdoc);
            }
            
            return result;
        } catch (SolrServerException e) {
            throw new EvalException(e);
        } finally {
            ctx.leave();
        }
    }
    
    public static class Result
    {
        private SolrDocumentList documents;
        private QueryResponse response;
        private String query;
        
        public Result(String query, QueryResponse response)
        {
            documents = new SolrDocumentList();
            this.query = query;
            this.response = response;
        }
        
        public String getQuery()
        {
            return query;
        }
        
        public QueryResponse getResponse()
        {
            return response;
        }
        
        public SolrDocumentList getDocuments()
        {
            return documents;
        }
        
        public void addDocument(SolrDocument doc)
        {
            documents.add(doc);
        }
    }
}
