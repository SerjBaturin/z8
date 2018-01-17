Z8.define('Z8.list.Item', {
	extend: 'Z8.Component',

	/*
	* config:
	*     active: false, 
	*     icon: '',
	*     fields: [] | '',
	*     checked: false,
	*
	* private:
	*     list: false,
	*     icon: null,
	*     item: null,
	*     handlers: null
	*/

	collapsed: false,

	hidden: 0,

	initComponent: function() {
		this.callParent();

		var record = this.record; 

		if(record != null) {
			this.level = record.get('level');
			this.children = record.get('hasChildren');

			var icon = record.get('icon');
			this.icon = icon != null ? icon : this.icon;

			if(record.on != null)
				record.on('change', this.onRecordChange, this);
		}
	},

	isReadOnly: function() {
		var record = this.record;
		return record != null ? record.getLock() != RecordLock.None : false;
	},

	onRecordChange: function(record, modified) {
		var fields = this.list.getFields();
		for(var i = 0, length = fields.length; i < length; i++) {
			var field = fields[i];
			var name = field.name;
			if(modified.hasOwnProperty(name))
				this.setText(i, this.formatText(field, record.get(name)));
		}

		if(this.list.locks)
			DOM.swapCls(this.lockIcon, this.isReadOnly(), 'fa-lock', '');
	},

	columnsMarkup: function() {
		var record = this.record;

		var columns = [];
		if(this.list.checks) {
			var check = { tag: 'i', cls: 'fa ' + (this.checked ? 'fa-check-square' : 'fa-square-o')};
			var text = { cls: 'text', cn: [check] };
			columns.push({ tag: 'td', cls: 'check column', cn: [text] });
		}

		if(this.list.locks) {
			var lock = { tag: 'i', cls: 'fa ' + (this.isReadOnly() ? 'fa-lock' : '')};
			var text = { cls: 'text', cn: [lock] };
			columns.push({ tag: 'td', cls: 'lock column', cn: [text] });
		}

		var icons = [];

		if(this.icon != null) {
			var iconCls = this.setIcon(this.icon);
			var icon = { tag: 'i', cls: iconCls.join(' '), html: String.htmlText() };
			icons.push(icon);
		}

		return columns.concat(this.fieldsMarkup(icons, record));
	},

	fieldsMarkup: function(icons, record) {
		var columns = [];

		var cls = 'column';

		if(record != null) {
			var fields = this.list.getFields();

			var hasChildren = this.hasChildren();

			if(hasChildren != null) {
				var level = this.getLevel();
				var collapserIcon = { tag: 'i', cls: (hasChildren ? 'fa fa-chevron-down ' : '') + 'icon', html: String.htmlText() };
				var collapser = { tag: 'span', cls: 'collapser tree-level-' + (hasChildren ? level : level + 1), cn: [collapserIcon] };
				icons.insert(collapser, 0);
			}

			for(var i = 0, length = fields.length; i < length; i++) {
				var field = fields[i];
				var title = null;
				var text = this.formatText(field, record.get(field.name));

				var type = field.type;

				if(String.isString(text)) {
					title = type != Type.Text ? Format.htmlEncode(text) : '';
					text = String.htmlText(text);
				}

				text = { tag: 'div', cls: 'text', cn: i == 0 ? icons.concat([text]) : [text] };

				columns.push({ tag: 'td', cls: cls + (type != null ? ' ' + type : ''), field: i, cn: [text], title: title });
			}
		} else {
			var text = String.htmlText(this.text);
			text = { tag: 'div', cls: 'text', cn: icons.concat([text]) };
			columns.push({ tag: 'td', cls: cls, cn: [text], title: this.text || '' });
		}

		return columns;
	},

	formatText: function(field, value) {
		if(field.renderer != null)
			return field.renderer.call(field, value);

		switch(field.type) {
		case Type.Date:
			return Format.date(value, field.format);
		case Type.Datetime:
			return Format.datetime(value, field.format);
		case Type.Integer:
			return Format.integer(value, field.format);
		case Type.Float:
			return Format.float(value, field.format);
		case Type.Boolean:
			return { tag: 'i', cls: value ? 'fa fa-check-square' : 'fa fa-square-o' };
		default:
			return value || '';
		}
	},

	htmlMarkup: function() {
		var cls = ['item'];

		if(!this.enabled)
			cls.push(['disabled']);

		if(this.active)
			cls.pushIf('active');

		return { tag: 'tr', id: this.getId(), cls: cls.join(' '), tabIndex: this.getTabIndex(), cn: this.columnsMarkup() };
	},

	completeRender: function() {
		this.callParent();

		this.collapser = this.selectNode('.item .collapser .icon');
		this.iconElement = this.selectNode('.item .icon');
		this.checkIcon = this.selectNode('.item>.column.check>.text>.fa');
		this.checkElement = this.selectNode('.item>.column.check');
		this.lockIcon = this.selectNode('.item>.column.lock>.text>.fa');
		this.cells = this.queryNodes('.item>.column:not(.check):not(.lock)');

		DOM.on(this, 'mouseDown', this.onMouseDown, this);
		DOM.on(this, 'click', this.onClick, this);
		DOM.on(this, 'dblClick', this.onDblClick, this);
		DOM.on(this, 'keyDown', this.onKeyDown, this);
	},

	onDestroy: function() {
		DOM.un(this, 'mouseDown', this.onMouseDown, this);
		DOM.un(this, 'click', this.onClick, this);
		DOM.un(this, 'dblClick', this.onDblClick, this);
		DOM.un(this, 'keyDown', this.onKeyDown, this);

		if(this.record != null && this.record.un != null)
			this.record.un('change', this.onRecordChange, this);

		this.collapser = this.iconElement = this.checkIcon = this.checkElement = this.lockIcon = this.cells = null;

		this.callParent();
	},

	setTabIndex: function(tabIndex) {
		tabIndex = this.callParent(tabIndex);
		DOM.setTabIndex(this, tabIndex);
		return tabIndex;
	},

	setActive: function(active) {
		this.active = active;
		DOM.swapCls(this, active, 'active');
	},

	focus: function() {
		return this.enabled ? DOM.focus(this) : false;
	},

	toggleCheck: function() {
		var checked = !this.checked;
		this.setChecked(checked);
		this.list.onItemCheck(this, checked);
	},

	isChecked: function() {
		return this.checked;
	},

	setChecked: function(checked) {
		this.checked = checked;
		DOM.swapCls(this.checkIcon, checked, 'fa-check-square', 'fa-square-o');
	},

	setIcon: function(icon) {
		this.icon = icon;
		var cls = this.iconCls = DOM.parseCls(icon).pushIf('fa', 'fa-fw', 'icon');
		DOM.setCls(this.iconElement, this.iconCls);
		return cls;
	},

	setEnabled: function(enabled) {
		DOM.swapCls(this, !enabled, 'disabled');
		this.callParent(enabled);
	},

	getValue: function() {
		var record = this.record;
		return record != null ? record.id : this.getId();
	},

	getText: function(field) {
		var record = this.record;
		return record != null ? record.get(field) || '' : this.text;
	},

	hasChildren: function() {
		return this.children;
	},

	isRoot: function() {
		return this.level == 0;
	},

	isCollapsed: function() {
		return this.collapsed === true;
	},

	isExpanded: function() {
		return this.children && !this.collapsed;
	},

	getLevel: function() {
		return this.level;
	},

	hide: function(hide) {
		if(this.hidden == 0 && hide)
			DOM.addCls(this, 'display-none');
		else if(this.hidden == 1 && !hide)
			DOM.removeCls(this, 'display-none');

		this.hidden = Math.max(this.hidden + (hide ? 1 : -1), 0);
	},

	collapse: function(collapsed) {
		if(this.collapsed != collapsed) {
			this.collapsed = collapsed;
			DOM.rotate(this.collapser, collapsed ? -90 : 0);
			this.list.onItemCollapse(this, collapsed);
		}
	},

	setText: function(index, text) {
		var cell = this.cells[index];
		if(String.isString(text)) {
			DOM.setValue(cell.firstChild.lastChild || cell.firstChild, String.htmlText(text));
			DOM.setAttribute(cell, 'title', text);
		} else {
			DOM.setInnerHTML(cell.firstChild, DOM.markup(text));
			DOM.setAttribute(cell, 'title', '');
		}
	},

	onMouseDown: function(event, target) {
		var dom = DOM.get(this);

		if(DOM.selectNode(dom.parentNode, 'tr:focus') != dom || !this.isEnabled())
			return;

		if(target == this.collapser || this.list.checks && DOM.isParentOf(this.checkElement, target))
			return;

		var index = this.findCellIndex(target);
		if(this.startEdit(index))
			event.stopEvent();
	},

	onClick: function(event, target) {
		event.stopEvent();

		if(!this.isEnabled())
			return;

		if(target == this.collapser)
			this.collapse(!this.collapsed);
		else if(this.list.checks && DOM.isParentOf(this.checkElement, target)) {
			this.toggleCheck();
			this.list.onItemClick(this, -1);
		} else {
			var index = this.findCellIndex(target);
			this.list.onItemClick(this, index);
		}
	},

	onDblClick: function(event, target) {
		event.stopEvent();

		if(!this.enabled)
			return;

		var index = this.findCellIndex(target);
		var field = index != -1 ? this.list.getFields()[index] : null;

		if(field == null || !DOM.isParentOf(field.editor, target))
			this.list.onItemDblClick(this, index);
	},

	onKeyDown: function(event, target) {
		var list = this.list;
		if(list.isEditing())
			return;

		var key = event.getKey();

		if(key == Event.ENTER) {
			if(this.useENTER) {
				list.onItemClick(this, -1);
				event.stopEvent();
			}
		} else if(key == Event.SPACE) {
			if(list.checks) {
				event.stopEvent();
				this.toggleCheck();
			} else {
				list.onItemClick(this, -1);
				event.stopEvent();
			}
		}
	},

	startEdit: function(index) {
		if(!this.active || index == -1)
			return false;
		return this.list.onItemStartEdit(this, index);
	},

	getCell: function(index) {
		return this.cells[index];
	},

	findCell: function(target) {
		var index = this.findCellIndex(target);
		return index != -1 ? this.cells[index] : null;
	},

	findCellIndex: function(target) {
		var cells = this.cells;

		for(var i = 0, length = cells.length; i < length; i++) {
			var cell = cells[i];
			if(DOM.isParentOf(cell, target))
				return i;
		}

		return -1;
	}
});