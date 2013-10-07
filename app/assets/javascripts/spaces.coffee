class Space extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
	events:
		"click .deleteSpace": "deleteSpace"
		"keyup .recordSearch": "recordSearch"
	deleteSpace: (e) ->
		e.preventDefault()
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
				$(".searchResults", @el).replaceWith(data)
			error: (err) ->
				console.error("Record search failed.")
				console.error(err.responseText)

class SpaceTab extends Backbone.View
	initialize: ->
		@id = @el.attr("tab-id")
		@name = $(".spaceName", @el).editInPlace
			context: this
			onChange: @renameSpace
		if @el.hasClass("active")
			@loadSpaceRecords()
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
				@records = _.filter window.records, (record) -> record._id in data
				@loadSpace()
			error: (err) ->
				console.error("Error when loading spaces.")
				console.error(err.responseText)
	loadSpace: ->
		loadFilters(@records, @id)
		postForm(@records, @id)

# Instantiate views
$ ->
	_.each $(".spaceTab"), (spaceTab) -> new SpaceTab el: $ spaceTab
	_.each $(".space"), (space) -> new Space el: $ space
	
	# Load all records and default space
	window.records = []
	spaceId = "default"
	jsRoutes.controllers.Spaces.loadAllRecords().ajax
		context: this
		success: (data) ->
			window.records = data
			
			# Load the filters
			loadFilters(window.records, spaceId)
			
			# Load the space
			postForm(window.records, spaceId)
			
			###
			json = JSON.stringify({"spaceId": null, "records": data})
			jsRoutes.controllers.Visualizations.jsonList().ajax
				context: this
				type: "POST"
				contentType: "application/json; charset=utf-8"
				data: json
				success: (response) ->
					console.log(response)
					#$("#space-default").html(response)
					$("#iframe-default").contents().find("html").html(response)
				error: (err) ->
					console.error("Error when loading visualization")
					console.error(err.responseText)
			###
			
		error: (err) ->
			console.error("Error when loading records.")
			console.error(err.responseText)

# General functions
postForm = (records, spaceId) ->
	$("#form-"+spaceId).empty()
	$("#form-"+spaceId).append('<input type="hidden" name="spaceId" value="' + spaceId + '">')
	_.each records, (record) ->
		$("#form-"+spaceId).append('<input type="hidden" name="' + record._id + ' creator" value="' + record.creator + '">')
		$("#form-"+spaceId).append('<input type="hidden" name="' + record._id + ' owner" value="' + record.owner + '">')
		$("#form-"+spaceId).append('<input type="hidden" name="' + record._id + ' created" value="' + record.created + '">')
		$("#form-"+spaceId).append('<input type="hidden" name="' + record._id + ' data" value="' + record.data + '">')
	$("#form-"+spaceId).submit()
	
filterRecords = (records, spaceId) ->
	creator = $("#filterCreator-"+spaceId).attr("value")
	owner = $("#filterOwner-"+spaceId).attr("value")
	records = filterByProperty(records, "creator", creator)
	records = filterByProperty(records, "owner", owner)
	postForm(records, spaceId)

filterByProperty = (list, property, value) ->
	return _.filter list, (record) -> if value is "any" then true else record[property] is value

loadFilters = (records, spaceId) ->
	creators = []
	owners = []
	_.each records, (record) ->
		creators.push record.creator
		owners.push record.owner
	# Load the names (synchronously; needed afterwards)
	ids = _.union(creators, owners)
	idsToNames = {}
	_.each ids, (id) ->
		jsRoutes.controllers.api.UserInfo.getName(id).ajax
			async: false
			success: (name) ->
				idsToNames[id] = name
			error: (err) ->
				console.error("Error when retrieving a user's name.")
				console.error(err.responseText)
	
	# Add filter options to select
	$("#filterCreator-"+spaceId).empty()
	$("#filterOwner-"+spaceId).empty()
	$("#filterCreator-"+spaceId).append('<option value="any">anyone</option>')
	$("#filterOwner-"+spaceId).append('<option value="any">anyone</option>')
	_.each (_.uniq creators), (creator) -> $("#filterCreator-"+spaceId).append('<option value="' + creator + '">' + idsToNames[creator] + '</option>')
	_.each (_.uniq owners), (owner) -> $("#filterOwner-"+spaceId).append('<option value="' + owner + '">' + idsToNames[owner] + '</option>')
	
	# Register the filter events
	$("#filterCreator-"+spaceId).on "change", (e) ->
		filterRecords(records, spaceId)
	$("#filterOwner-"+spaceId).on "change", (e) ->
		filterRecords(records, spaceId)
