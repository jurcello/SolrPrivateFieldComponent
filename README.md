SolrPrivateFieldComponent
=========================

The SolrPrivateFieldComponent is a component for apache solr by which some of the fields in the search index might be searched in, but will never be returned in the search result.

Usage
=====

Copy the jar file in the lib dir of your solr core. 
Then add the following snippet to the solrconfig.xml:

	  <searchComponent name="privateField" class="nl.triquanta.solr.component.PrivateFieldComponent">
        <str name="allowedFields">allowedField1, allowedField2</str>
        <str name="privateField">fieldContainingPrivateInformation</str>
        <str name="publicValue">full</str>
      </searchComponent>

There are 3 parameters in this declaration:

	1. allowedFields: the fields that may be returned if the document is regarded private.
	2. privateField: the field in the index in which the information about the privacy of the document is stored.
	3. publicValue: the value of the privateField for which the document is regarded public.

After declaration of the component, add it to the components list of the request handler. For example:

	  <requestHandler name="myExampleHandler" class="solr.SearchHandler">
	    <!-- default values for query parameters -->
	    <lst name="defaults">
	    	.. all the defaults you like
	    </lst>
	    <arr name="components">
	      <str>query</str>
	      <str>facet</str>
	      <str>mlt</str>
	      <str>privateField</str>
	      <str>highlight</str>
	      <str>stats</str>
	      <str>debug</str>
	    </arr>
	  </requestHandler>


Example
=======

Assume we have the following documents:

	document1:
		title: my secret document
		id: 200
		secret: secret info which may not be returned
		access: private

	document2: 
		title: my public document
		id: 201
		secret: information which is not really secret
		access: full

And we have the following component definition:

	  <searchComponent name="privateField" class="nl.triquanta.solr.component.PrivateFieldComponent">
        <str name="allowedFields">title</str>
        <str name="privateField">secret</str>
        <str name="publicValue">full</str>
      </searchComponent>

Using this definition, if we do search request and let all fields be returned, we get the following result:

	results:
		document1:
			title: my secret document
		document2: 
			title: my public document
			id: 201
			secret: information which is not really secret