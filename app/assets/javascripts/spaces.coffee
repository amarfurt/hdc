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
			
# Instantiate views
$ ->
	_.map($(".spaceTab"), (spaceTab) -> new SpaceTab el: $ spaceTab)
	_.map($(".space"), (space) -> new Space el: $ space)
