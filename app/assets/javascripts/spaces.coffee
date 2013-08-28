class Space extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
	events:
		"click .deleteSpace": "deleteSpace"
	deleteSpace: (e) ->
    	e.preventDefault()
    	@loading(true)
    	jsRoutes.controllers.Spaces.delete(@id).ajax
    		context: this
    		success: ->
    			@el.remove()
    			$(".spaceTab.active").remove()
    			@loading(false)
    			$("#spaceTabs").children("li:first").addClass("active")
    			$("#spaceContent").children("div:first").addClass("active in")
    		error: (err) ->
    			@loading(false)
    			$.error("Error: " + err)
    loading: (display) ->
    	if (display)
    		@el.children(".addRecord").hide()
    		@el.children(".deleteSpace").hide()
    	else
    		@el.children(".addRecord").show()
    		@el.children(".deleteSpace").show()

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
				alert err.responseText
				$.error("Error: " + err)

class SpaceTabs extends Backbone.View
	initialize: ->
		@el.children("li:first").addClass("active")
		@el.children(".spaceTab").each (i, spaceTab) ->
			new SpaceTab el: $(spaceTab)

class SpaceContent extends Backbone.View
	initialize: ->
		@el.children("div:first").addClass("active in")
		@el.children(".space").each (i, space) ->
			new Space el: $(space)

# Instantiate views
$ ->
	tabs = new SpaceTabs el: $("#spaceTabs")
	cont = new SpaceContent el: $("#spaceContent")
