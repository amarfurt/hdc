class oldCircle extends Backbone.View
	initialize: ->
		@id = @el.attr("circle-id")
		@name = $(".circleName", @el).editInPlace
			context: this
			onChange: @renameCircle
		$(".member", @el).each (i, member) ->
				new CircleMember el: $(member)
	renameCircle: (name) ->
		@loading(true)
		jsRoutes.controllers.Circles.rename(@id).ajax
			context: this
			data:
				name: name
			success: (data) ->
				@loading(false)
				@name.editInPlace("close", data)
			error: (err) ->
				@loading(false)
				alert err.responseText
				$.error("Error: " + err)
	events:
		"click .deleteCircle": "deleteCircle"
		"click .addMember":	"addMember"
	deleteCircle: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Circles.delete(@id).ajax
			context: this
			success: ->
				@el.remove()
				@loading(false)
			error: (err) ->
				@loading(false)
				$.error("Error: " + err)
	loading: (display) ->
		if (display)
			@el.children(".addMember").hide()
			@el.children(".deleteCircle").hide()
		else
			@el.children(".addMember").show()
			@el.children(".deleteCircle").show()
	addMember: (e) ->
		e.preventDefault()
		@loadingMember(true)
		@member = $(".newMember", @el).editInPlace
			context: this
			onChange: @addNewMember
		$(".newMember", @el).editInPlace("edit")
	addNewMember: (member) ->
		jsRoutes.controllers.Circles.addMember(@id).ajax
			context: this
			data:
				name: member
			success: (data) ->
				@member.editInPlace("close", "New member")
				@loadingMember(false)
				_view = new CircleMember
					el: $(data).appendTo("#"+@id+".list-group")
				$("#"+@id+".noMembers", @el).addClass("hidden")
			error: (err) ->
				@member.editInPlace("close", "New member")
				@loadingMember(false)
				alert err.responseText
				$.error("Error: " + err.responseText)
	loadingMember: (display) ->
		if (display)
			$(".addMember", @el).hide()
			$(".newMember", @el).show()
		else
			$(".newMember", @el).hide()
			$(".addMember", @el).show()

### NEW ###	
class Member extends Backbone.View
	initialize: ->
		@circleId = @el.attr("circle-id")
		@id = @el.attr("id")
	events:
		"click .removeMember": "removeMember"
	removeMember: (e) ->
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
				console.error("Failed to remove member.")
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
		"keyup .memberSearch": "memberSearch"
	deleteCircle: (e) ->
		@loading(true)
		jsRoutes.controllers.Circles.delete(@id).ajax
			context: this
			error: (err) ->
				@loading(false)
				$.error("Error: " + err)
	loading: (display) ->
		if (display)
			@el.children(".addMember").hide()
			@el.children(".deleteCircle").hide()
		else
			@el.children(".addMember").show()
			@el.children(".deleteCircle").show()
	memberSearch: (e) ->
		search = $(".memberSearch", @el).val()
		jsRoutes.controllers.Circles.searchMembers(@id, search).ajax
			context: this
			success: (data) ->
				$(".memberForm", @el).replaceWith(data)
			error: (err) ->
				console.error("Member search failed.")
				console.error(err)

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
				console.error(err)

# Instantiate views
$ ->
	_.map($(".circleTab"), (circleTab) -> new CircleTab el: $ circleTab)
	_.map($(".circle"), (circle) -> new Circle el: $ circle)
