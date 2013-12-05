class MarketController extends Backbone.View
	initialize: ->
		@appId = $("#installButton").attr("app-id")
		@visualizationId = $("#installButton").attr("visualization-id")
	events:
		"click #installButton": "install"
		"click #uninstallButton": "uninstall"
	install: (e) ->
		e.preventDefault()
		$("#installButton").prop("disabled", true)
		if (@appId)
			jsRoutes.controllers.Market.installApp(@appId).ajax
				success: @installSuccess
				error: @installError
		else if (@visualizationId)
			jsRoutes.controllers.Market.installVisualization(@visualizationId).ajax
				success: @installSuccess
				error: @installError
	installSuccess: (data) ->
		$("#installButton").addClass("hidden")
		$("#installButton").prop("disabled", false)
		$("#uninstallButton").removeClass("hidden")
		$("#redirectNotice").removeClass("hidden")
	installError: (err) ->
		console.error("Error installing.")
		console.error(err.responseText)
		$("#installButton").prop("disabled", false)
	uninstall: (e) ->
		e.preventDefault()
		$("#uninstallButton").prop("disabled", true)
		if (@appId)
			jsRoutes.controllers.Market.uninstallApp(@appId).ajax
				success: @uninstallSuccess
				error: @uninstallError
		else if (@visualizationId)
			jsRoutes.controllers.Market.uninstallVisualization(@visualizationId).ajax
				success: @uninstallSuccess
				error: @uninstallError
	uninstallSuccess: ->
		$("#uninstallButton").addClass("hidden")
		$("#uninstallButton").prop("disabled", false)
		$("#installButton").removeClass("hidden")
	uninstallError: (err) ->
		console.error("Error installing.")
		console.error(err.responseText)
		$("#uninstallButton").prop("disabled", false)

# jQuery
$ ->
	# Instantiate views
	# TODO separate visualization view from market?
	new MarketController el: $ "body"
