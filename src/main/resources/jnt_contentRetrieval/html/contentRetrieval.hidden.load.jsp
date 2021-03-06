<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>


<%-- Get parameters of the module --%>
<jcr:nodeProperty node="${currentNode}" name='jcr:title' var="title"/>
<jcr:nodeProperty node="${currentNode}" name='maxItems' var="nbOfResult"/>
<jcr:nodeProperty node="${currentNode}" name='j:startNode' var="startNode"/>
<jcr:nodeProperty node="${currentNode}" name='j:criteria' var="criteria"/>
<jcr:nodeProperty node="${currentNode}" name='j:sortDirection' var="sortDirection"/>
<jcr:nodeProperty node="${currentNode}" name='j:type' var="type"/>
<jcr:nodeProperty node="${currentNode}" name="j:filter" var="filters"/>
<jcr:nodeProperty node="${currentNode}" name="j:subNodesView" var="subNodesView"/>
<jcr:nodeProperty node="${currentNode}" name='j:noResultsMessage' var="noResultsMessage"/>

<%-- Display title (if it is skinnable the title is displayed with skin--%>
<c:if test="${not empty title and not empty title.string and not jcr:isNodeType(currentNode, 'jmix:skinnable')}">
     <h3>${title.string}</h3>
</c:if>
<c:if test="${not empty startNode}">
    <c:set var="startNode" value="${startNode.node}"/>
</c:if>
<c:if test="${empty startNode}">
    <c:set var="startNode" value="${jcr:getMeAndParentsOfType(renderContext.mainResource.node, 'jnt:page')[0]}"/>
</c:if>

<%-- Define the query, depending on the selected criteria --%>
<query:definition var="listQuery" limit="${currentResource.moduleParams.queryLoadAllUnsorted == 'true' ? -1 : nbOfResult.long}">
    <query:selector nodeTypeName="${type.string}"/>
    <query:descendantNode path="${startNode.path}"/>
    <query:or>
        <c:forEach var="filter" items="${filters}">
            <c:if test="${not empty filter.string}">
                <query:equalTo propertyName="j:defaultCategory" value="${filter.string}"/>
            </c:if>
        </c:forEach>
    </query:or>
    <c:if test="${not currentResource.moduleParams.queryLoadAllUnsorted == 'true'}">
        <query:sortBy propertyName="${criteria.string}" order="${sortDirection.string}"/>
    </c:if>
</query:definition>

<%-- Debug message --%>
<%-- <p>Debug > Nb of result from query (Criteria : ${criteria.string} - Nb of result : ${nbOfResult.long} - Mode : ${mode.string}) : ${fn:length(result.nodes)}</p>  --%>

<%-- Set variables to store the result --%>
<c:set target="${moduleMap}" property="editable" value="false" />
<c:set target="${moduleMap}" property="listQuery" value="${listQuery}" />
<c:set target="${moduleMap}" property="emptyListMessage">
   <c:choose>
   <c:when test="${not empty noResultsMessage}">
      ${noResultsMessage.string}
   </c:when>
   <c:otherwise>
       <fmt:message key='noResults.message'/>
   </c:otherwise>    
</c:choose>
</c:set>
<c:set target="${moduleMap}" property="subNodesView" value="${subNodesView.string}" />
<template:addCacheDependency flushOnPathMatchingRegexp="\\\\Q${startNode.path}\\\\E/.*"/>
