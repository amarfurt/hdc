class Record extends Backbone.View
	initialize: ->
		@id = @el.attr("record-id")
	events:
		"click .spacesButton": "findSpaces"
		"click .circlesButton": "findCircles"
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
	findCircles: ->
		jsRoutes.controllers.Spaces.findCirclesWith(@id).ajax
			context: this
			success: (data) ->
				_.each $(":checkbox"), (checkbox) -> $(checkbox).prop("checked", false)
				_.each data, (id) -> $(":checkbox[name=" + id + "]").prop("checked", true)
				window.cur = @id
			error: (err) ->
				console.error("Error when finding circles with record.")
				console.error(err.responseText)
	updateSpaces: ->
		if window.cur == @id
			spaces = []
			_.each $(":checkbox"), (checkbox) -> spaces.push $(checkbox).attr("name") if $(checkbox).prop("checked")
			jsRoutes.controllers.Spaces.updateSpaces(@id, spaces).ajax
				context: this
				success: ->
					# if record has been removed from this space, remove it from the visualization
					if window.spaceId not in spaces
						$("[record-id=" + window.cur + "]").remove()
					# TODO: record is not removed from records list passed to Spaces yet...
				error: (err) ->
					console.error("Updating the spaces of this record failed.")
					console.error(err.responseText)
	updateCircles: ->
		if window.cur == @id
			circles = []
			_.each $(":checkbox"), (checkbox) -> circles.push $(checkbox).attr("name") if $(checkbox).prop("checked")
			jsRoutes.controllers.Spaces.updateCircles(@id, circles).ajax
				context: this
				error: (err) ->
					console.error("Updating the circles of this record failed.")
					console.error(err.responseText)
					
class List extends Backbone.View
	initialize: ->
		window.spaceId = $("[space-id]").attr("space-id")
		window.cur = null
		@records = _.map $(".record"), (record) -> new Record el: $ record
	events:
		"click #updateSpaces": "updateSpaces"
		"click #updateCircles": "updateCircles"
	updateSpaces: ->
		_.each @records, (record) -> record.updateSpaces()
	updateCircles: ->
		_.each @records, (record) -> record.updateCircles()

# Instantiate views
$ ->
	new List el: $ "body"
