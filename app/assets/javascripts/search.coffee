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
	globalSearch: (e) ->
		# typeahead somehow disables the form submit on pressing enter...
		if e.keyCode is 13 then $("#globalSearchForm").submit()

# Instantiate views
$ ->
	new SearchController el: $ ".navbar-form"