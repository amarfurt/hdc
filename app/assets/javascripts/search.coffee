class SearchController extends Backbone.View
	initialize: ->
		$("#globalSearch").typeahead({
			name: "records",
			remote: {
				url: null
				replace: (url, query) ->
					jsRoutes.controllers.Search.complete(query).url
			}
		})
	events:
		"keyup #globalSearch": "globalSearch"
		"typeahead:selected #globalSearch": "selected" 
	globalSearch: (e) ->
		# typeahead somehow disables the form submit on pressing enter...
		if e.keyCode is 13 then $("#globalSearchForm").submit()
	selected: (e, datum) ->
		if datum.type isnt "other"
			window.location.href = jsRoutes.controllers.Search.show(datum.type, datum.id).url

# Instantiate views
$ ->
	new SearchController el: $ ".navbar-form"