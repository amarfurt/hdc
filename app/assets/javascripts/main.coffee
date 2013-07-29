$.fn.editInPlace = (method, options...) ->
    this.each ->
        methods = 
            # public methods
            init: (options) ->
                valid = (e) =>
                    newValue = @input.val()
                    options.onChange.call(options.context, newValue)
                cancel = (e) =>
                    @el.show()
                    @input.hide()
                @el = $(this).dblclick(methods.edit)
                @input = $("<input type='text' />")
                    .insertBefore(@el)
                    .keyup (e) ->
                        switch(e.keyCode)
                            # Enter key
                            when 13 then $(this).blur()
                            # Escape key
                            when 27 then cancel(e)
                    .blur(valid)
                    .hide()
            edit: ->
                @input
                    .val(@el.text())
                    .show()
                    .focus()
                    .select()
                @el.hide()
            close: (newName) ->
                @el.text(newName).show()
                @input.hide()
        # jQuery approach: http://docs.jquery.com/Plugins/Authoring
        if ( methods[method] )
            return methods[ method ].apply(this, options)
        else if (typeof method == 'object')
            return methods.init.call(this, method)
        else
            $.error("Method " + method + " does not exist.")

class Drawer extends Backbone.View
	initialize: ->
		@el.children("li").each (i, circle) ->
			new Circle
				el: $(circle)
            
class Circle extends Backbone.View
	initialize: ->
        @id = @el.attr("circle-id")
        @name = $(".circleName", @el).editInPlace
            context: this
            onChange: @renameCircle
	renameCircle: (name) ->
        jsRoutes.controllers.Circles.rename(@id).ajax
            context: this
            data:
                name: name
            success: (data) ->
                @loading(false)
                @name.editInPlace("close", data)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
	events:
        "click .deleteCircle": "deleteCircle"
    deleteCircle: (e) ->
    	jsRoutes.controllers.Circles.delete(@id).ajax
            context: this
            success: ->
                alert "Circle deleted. Please refresh the page."
            error: (err) ->
                $.error("Error: " + err)

class CircleManager extends Backbone.View
	events:
        "click #addCircle": "addCircle"
    addCircle: (e) ->
    	jsRoutes.controllers.Circles.add().ajax
            context: this
            success: (data) ->
            	_view = new Circle
                    el: $(data).appendTo("#circles")
                _view.el.find(".circleName").editInPlace("edit")
            error: (err) ->
                $.error("Error: " + err)

# Instantiate views
$ -> 
    drawer = new Drawer el: $("#circles")
$ ->
	mngr = new CircleManager el: $("#manage-circles")
        