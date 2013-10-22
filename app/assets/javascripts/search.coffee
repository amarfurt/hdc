class SearchController extends Backbone.View
	events:
		"keyup #globalSearch": "globalSearch"
		"change #searchDomain": "globalSearch"
	globalSearch: ->
		# special treatment on the first keypress of a query
		if $("#search-content").exists()
			$("#search-content").html("Loading search results...")
		else
			$("#page-content").html("Loading search results...")
			$(".nav > .active").removeClass("active")
		search = $("#globalSearch").val()
		domain = $("#searchDomain").attr("value")
		jsRoutes.controllers.Search.find(search, domain).ajax
			context: this
			success: (data) ->
				$("#page-content").html(data)
			error: (err) ->
				console.error("Error in global search.")
				console.error(err.responseText)

# Instantiate views
$ ->
	new SearchController el: $ ".navbar-form"