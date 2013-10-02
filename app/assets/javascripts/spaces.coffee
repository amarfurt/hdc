class Record extends Backbone.View
	initialize: ->
		@spaceId = @el.attr("space-id")
		@id = @el.attr("id")
	events:
		"click .removeRecord": "removeRecord"
	removeRecord: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Spaces.removeRecord(@spaceId).ajax
			context: this
			data:
				id: @id
			success: (data) ->
				@el.remove()
				@loading(false)
			error: (err) ->
				@loading(false)
				alert err.responseText
				$.error("Error: " + err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".removeRecord").hide()
		else
			@el.children(".removeRecord").show()

class Space extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
		$(".record", @el).each (i, record) ->
			new Record el: $(record)
	events:
		"click .deleteSpace": "deleteSpace"
		"keyup .recordSearch": "recordSearch"
	deleteSpace: (e) ->
		@loading(true)
		jsRoutes.controllers.Spaces.delete(@id).ajax
			context: this
			success: (response) ->
				@loading(false)
				window.location.replace(response)
			error: (err) ->
				@loading(false)
				console.error("Deleting space failed.")
				console.error(err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".addRecord").hide()
			@el.children(".deleteSpace").hide()
		else
			@el.children(".addRecord").show()
			@el.children(".deleteSpace").show()
	recordSearch: (e) ->
		search = $(".recordSearch", @el).val()
		jsRoutes.controllers.Spaces.searchRecords(@id, search).ajax
			context: this
			success: (data) ->
				$(".recordForm", @el).replaceWith(data)
			error: (err) ->
				console.error("Record search failed.")
				console.error(err.responseText)

class SpaceTab extends Backbone.View
	initialize: ->
		@id = @el.attr("tab-id")
		@name = $(".spaceName", @el).editInPlace
			context: this
			onChange: @renameSpace
	events:
		"click": "loadSpaceRecords"
	renameSpace: (name) ->
		jsRoutes.controllers.Spaces.rename(@id).ajax
			context: this
			data:
				name: name
			success: (data) ->
				@name.editInPlace("close", data)
			error: (err) ->
				console.error("Renaming space failed.")
				console.error(err.responseText)
	loadSpaceRecords: ->
		jsRoutes.controllers.Spaces.loadRecords(@id).ajax
			context: this
			success: (data) ->
				records = _.filter window.records, (record) -> record._id in data
				@loadSpace(records)
			error: (err) ->
				console.error("Error when loading spaces.")
				console.error(err.responseText)
	loadSpace: (records) ->
		$("#form-"+@id).empty()
		$("#form-"+@id).append('<input type="hidden" name="spaceId" value="' + @id + '">')
		_.each records, ((record) ->
			$("#form-"+@id).append('<input type="hidden" name="' + record._id + ' creator" value="' + record.creator + '">')
			$("#form-"+@id).append('<input type="hidden" name="' + record._id + ' owner" value="' + record.owner + '">')
			$("#form-"+@id).append('<input type="hidden" name="' + record._id + ' created" value="' + record.created + '">')
			$("#form-"+@id).append('<input type="hidden" name="' + record._id + ' data" value="' + record.data + '">')
			), this
		$("#form-"+@id).submit()

# Instantiate views
$ ->
	_.each $(".spaceTab"), (spaceTab) -> new SpaceTab el: $ spaceTab
	_.each $(".space"), (space) -> new Space el: $ space
	
	# Load all records and default space
	window.records = []
	jsRoutes.controllers.Spaces.loadAllRecords().ajax
		context: this
		success: (data) ->
			window.records = data
			_.each window.records, (record) ->
				$("#form-default").append('<input type="hidden" name="' + record._id + ' creator" value="' + record.creator + '">')
				$("#form-default").append('<input type="hidden" name="' + record._id + ' owner" value="' + record.owner + '">')
				$("#form-default").append('<input type="hidden" name="' + record._id + ' created" value="' + record.created + '">')
				$("#form-default").append('<input type="hidden" name="' + record._id + ' data" value="' + record.data + '">')
				$("#form-default").submit()
		error: (err) ->
			console.error("Error when loading records.")
			console.error(err.responseText)
	
	#*******************************************#
	#        Testing code snippets...           #
	#*******************************************#	
	getJson = () ->
		JSON.stringify({"record": "Hi there!"})
	
	loadVisualization = (e) ->
		e.preventDefault()
		jsRoutes.controllers.Visualization.list().ajax
			context: this
			contentType: "application/json; charset=utf-8"
			data:
				getJson()
			success: (response) ->
				console.log("Success!")
				console.log(response)
				#$("#iframe-default").attr("src", jsRoutes.controllers.Visualization.list().url)
			error: (err) ->
				console.error("Error when loading visualization.")
				console.error(err.responseText)

	#$("#form-default").on("submit", loadVisualization)
