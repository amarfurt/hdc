class Record extends Backbone.View
	initialize: ->
		@id = @el.attr("record-id")
	events:
		"click .spacesButton": "findSpaces"
	findSpaces: ->
		jsRoutes.controllers.Spaces.findSpacesWith(@id).ajax
			context: this
			success: (data) ->
				_.each $(":checkbox"), (checkbox) -> $(checkbox).prop("checked", false)
				_.each data, (id) -> $(":checkbox[name=" + id + "]").prop("checked", true)
			error: (err) ->
				console.error("Error when finding spaces with record.")
				console.error(err.responseText)

# Instantiate views
$ ->
	_.each $(".record"), (record) -> new Record el: $ record
