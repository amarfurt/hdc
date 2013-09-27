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
				window.cur = @id
			error: (err) ->
				console.error("Error when finding spaces with record.")
				console.error(err.responseText)
	updateSpaces: ->
		if window.cur == @id
			spaces = []
			_.each $(":checkbox"), (checkbox) -> spaces.push $(checkbox).attr("name") if $(checkbox).prop("checked")
			jsRoutes.controllers.Spaces.updateRecords(@id, spaces).ajax
				context: this
				success: (data) ->
					# TODO: if record has been removed from this space, remove it from the visualization
				error: (err) ->
					console.error("Updating the spaces of this record failed.")
					console.error(err.responseText)
					
class List extends Backbone.View
	initialize: ->
		window.cur = null
		@records = _.map $(".record"), (record) -> new Record el: $ record
	events:
		"click #updateSpaces": "updateSpaces"
	updateSpaces: ->
		_.each @records, (record) -> record.updateSpaces()

# Instantiate views
$ ->
	new List el: $ "body"
