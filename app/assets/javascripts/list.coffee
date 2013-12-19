class Record extends Backbone.View
	initialize: ->
		@id = @el.attr("data-rid")
	events:
		"click .spacesButton": "findSpaces"
		"click .circlesButton": "findCircles"
	findSpaces: ->
		jsRoutes.controllers.Records.findSpacesWith(@id).ajax
			context: this
			success: (data) ->
				_.each $(":checkbox"), (checkbox) -> $(checkbox).prop("checked", false)
				_.each data, (id) -> $(":checkbox[name=" + id + "]").prop("checked", true)
				@parent.curRecord = @id
			error: (err) ->
				console.error("Error when finding spaces with record.")
				console.error(err.responseText)
	findCircles: ->
		jsRoutes.controllers.Records.findCirclesWith(@id).ajax
			context: this
			success: (data) ->
				_.each $(":checkbox"), (checkbox) -> $(checkbox).prop("checked", false)
				_.each data, (id) -> $(":checkbox[name=" + id + "]").prop("checked", true)
				@prevCheckedCircles = data
				@parent.curRecord = @id
			error: (err) ->
				console.error("Error when finding circles with record.")
				console.error(err.responseText)
	updateSpaces: ->
		if @parent.curRecord == @id
			spaces = []
			_.each $(":checkbox"), (checkbox) -> spaces.push $(checkbox).attr("name") if $(checkbox).prop("checked")
			jsRoutes.controllers.Records.updateSpaces(@id, spaces).ajax
				context: this
				success: ->
					# if record has been removed from this space, remove it from the visualization
					if @parent.spaceId isnt "default" and @parent.spaceId not in spaces
						$("[data-rid=" + @parent.curRecord + "]").remove()
				error: (err) ->
					console.error("Updating the spaces of this record failed.")
					console.error(err.responseText)
	updateCircles: ->
		if @parent.curRecord == @id
			checkedCircles = []
			_.each $(":checkbox"), (checkbox) -> checkedCircles.push $(checkbox).attr("name") if $(checkbox).prop("checked")
			intersection = _.intersection @prevCheckedCircles, checkedCircles
			sharingStopped = _.difference @prevCheckedCircles, intersection
			sharingStarted = _.difference checkedCircles, intersection
			jsRoutes.controllers.Records.updateSharing(@id, sharingStarted, sharingStopped).ajax
				context: this
				error: (err) ->
					console.error("Updating the sharing settings of this record failed.")
					console.error(err.responseText)
					
class List extends Backbone.View
	initialize: ->
		# List.coffee is only used in records (former default space)
		@spaceId = "default"
		#@spaceId = $("[data-sid]").attr("data-sid")
		@curRecord = null
		@records = _.map $(".record"), (record) -> new Record el: $ record
		_.each @records, ((record) -> record.parent = this), this
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
