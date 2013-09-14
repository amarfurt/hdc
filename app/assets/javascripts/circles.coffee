class Member extends Backbone.View
	initialize: ->
		@circleId = @el.attr("circle-id")
		@id = @el.attr("id")
	events:
		"click .removeMember": "removeMember"
	removeMember: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Circles.removeMember(@circleId).ajax
			context: this
			data:
				name: @id
			success: (data) ->
				@el.remove()
				@loading(false)
			error: (err) ->
				@loading(false)
				console.error("Removing member failed.")
				console.error(err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".removeMember").hide()
		else
			@el.children(".removeMember").show()

class Circle extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
		$(".member", @el).each (i, member) ->
			new Member el: $(member)
	events:
		"click .deleteCircle": "deleteCircle"
		"keyup .userSearch": "userSearch"
	deleteCircle: (e) ->
		@loading(true)
		jsRoutes.controllers.Circles.delete(@id).ajax
			context: this
			success: (response) ->
				window.location.replace(response)
			error: (err) ->
				@loading(false)
				console.error("Deleting circle failed.")
				console.error(err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".addUser").hide()
			@el.children(".deleteCircle").hide()
		else
			@el.children(".addUser").show()
			@el.children(".deleteCircle").show()
	userSearch: (e) ->
		search = $(".userSearch", @el).val()
		jsRoutes.controllers.Circles.searchUsers(@id, search).ajax
			context: this
			success: (data) ->
				$(".userForm", @el).replaceWith(data)
			error: (err) ->
				console.error("User search failed.")
				console.error(err.responseText)

class CircleTab extends Backbone.View
	initialize: ->
		@id = @el.attr("tab-id")
		@name = $(".circleName", @el).editInPlace
			context: this
			onChange: @renameCircle
	renameCircle: (name) ->
		jsRoutes.controllers.Circles.rename(@id).ajax
			context: this
			data:
				name: name
			success: (data) ->
				@name.editInPlace("close", data)
			error: (err) ->
				console.error("Renaming circle failed.")
				console.error(err.responseText)

# Instantiate views
$ ->
	_.map($(".circleTab"), (circleTab) -> new CircleTab el: $ circleTab)
	_.map($(".circle"), (circle) -> new Circle el: $ circle)
