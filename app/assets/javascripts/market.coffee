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

# jQuery
$ ->
	# Instantiate views
	new VisualizationController el: $ "body"

	# Load apps
	
	# Load visualizations
	jsRoutes.controllers.Market.loadVisualizations().ajax
		success: (data) ->
			window.visualizations = data
		error: (err) ->
			console.error("Error loading visualizations.")
			console.error(err.responseText)
