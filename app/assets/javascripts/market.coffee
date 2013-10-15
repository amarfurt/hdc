class VisualizationController extends Backbone.View
	initialize: ->
		@id = $("#installButton").attr("visualization-id")
	events:
		"click #installButton": "install"
		"click #uninstallButton": "uninstall"
	install: (e) ->
		e.preventDefault()
		$("#installButton").prop("disabled", true)
		jsRoutes.controllers.Market.installVisualization(@id).ajax
			success: ->
				$("#installButton").addClass("hidden")
				$("#installButton").prop("disabled", false)
				$("#uninstallButton").removeClass("hidden")
				$("#redirectNotice").removeClass("hidden")
			error: (err) ->
				console.error("Error installing visualization.")
				console.error(err.responseText)
				$("#installButton").prop("disabled", false)
	uninstall: (e) ->
		e.preventDefault()
		$("#uninstallButton").prop("disabled", true)
		jsRoutes.controllers.Market.uninstallVisualization(@id).ajax
			success: ->
				$("#uninstallButton").addClass("hidden")
				$("#uninstallButton").prop("disabled", false)
				$("#installButton").removeClass("hidden")
			error: (err) ->
				console.error("Error installing visualization.")
				console.error(err.responseText)
				$("#uninstallButton").prop("disabled", false)

class MarketController extends Backbone.View
	events:
		"click #registerVisualization": "registerVisualization"
	registerVisualization: ->
		name = $("#newVisualizationName").val()
		description = $("#newVisualizationDescription").val()
		url = $("#newVisualizationURL").val()
		tags = $("#newVisualizationTags").val()
		jsRoutes.controllers.Market.registerVisualization(name, description, url, tags).ajax
			context: this
			success: (redirect) ->
				window.location.replace(redirect)
			error: (err) ->
				$("#errorMessage").html(err.responseText)
				$("#errorMessageAlert").removeClass("hidden")

# jQuery
$ ->
	# Instantiate views
	# TODO separate visualization view from market?
	new VisualizationController el: $ "body"
	new MarketController el: $ "body"

	# Load apps
	
	# Load visualizations
	jsRoutes.controllers.Market.loadVisualizations().ajax
		success: (data) ->
			window.visualizations = data
		error: (err) ->
			console.error("Error loading visualizations.")
			console.error(err.responseText)
