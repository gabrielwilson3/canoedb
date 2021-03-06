package io.github.gabrielwilson3.canoedb;

import java.util.*;
import java.io.*;
import java.net.*;

public class Response {
	
	int sessionId;
	Socket socket;
	
	// init
	public Response ( Socket s, int id ) {
		sessionId = id;
		socket = s;
	}
	
	// select the output format
	public void write ( Query q, String format ) {
		switch(format) {
			case "json" : outputJSON( q ); break;
			case "csv" : outputCSV( q ); break;
			case "form" : outputForm( q ); break;
			default : outputSPA(); break;
		}
	}
	
	// output to the socket
	void output ( String body, String mime ) {
		try {
			System.out.println("["+sessionId+"] Responce: writing to socket...");
			// Open an output stream to the socket
			PrintWriter out	= new PrintWriter(socket.getOutputStream(), true); // autoFlush true
			// Print the HTTP text string
			out.print(
				"HTTP/1.0 200 OK\r\n"+
				"Content-type: "+mime+"; charset=utf-8\r\n"+
				"Access-Control-Allow-Origin: *\r\n"+
				"\r\n"+
				body
			);
			// Close the output stream
			out.close();
			System.out.println("["+sessionId+"] Responce: finished writing");
		} catch (Exception e) {
			System.out.println("["+sessionId+"] Responce: ERROR writing to socket");
			e.printStackTrace();
		}
	}
	
	// JSON
	void outputJSON ( Query q ) {
		System.out.println("["+sessionId+"] Responce: generating JSON...");
		String body =
			"{\n"+
			"\"name\" : \""+
			q.db.name()+
			"\",\n"+
			"\"structure\" : "+
			q.structMap.toJSON()+
			",\n"+
			"\"columns\" : "+
			q.colMap.toJSON()+
			",\n"+
			"\"rows\" : "+
			q.rowMap.toJSON()+
			"\n}";
			
		output( body, "application/json" );
	}

	// CSV
	void outputCSV ( Query q ) {
		System.out.println("["+sessionId+"] Responce: generating CSV...");
		String csv = "CSV format not operational yet...";
		
		output( csv, "text/csv" );
	}
	
	// HTML Form
	void outputForm ( Query q ) {
		System.out.println("["+sessionId+"] Responce: generating form HTML...");
		// start HTML and start the form table
		String html = 
			"<html>\n<head>\n<title>CanoeDB</title>\n<style>\n"+
			"body { font-family:sans-serif; }"+
			"div { width:100%; overflow-x:auto; overflow-y:hidden; }\n"+
			"table { border-collapse:collapse; table-layout:fixed; }\n"+
			"th, td { padding:10px; text-align:left; }\n"+
			"</style></head>\n<body>\n<div>\n<form id=\"main_form\" method=\"post\">\n<table>\n<tr>\n";
		// loop through all the tables and columns
		for (String table : q.db.tables()) {
			Table t = q.db.table(table);
			for (String column : t.columns()) {
				if (t.reference(column).equals("")) {
					html +=
						"<td>"+table+"<br>"+column+"<br>"+
						"<input name=\""+table+"."+column+"\" list=\""+table+"."+column+"_list\" "+
						"value=\""+(q.inputTemplate.read(table, column)!=null ? q.inputTemplate.read(table, column) : "")+"\" "+
						"onchange=\"document.getElementById('main_form').submit()\" "+
						"onblur=\"document.getElementById('main_form').submit()\" "+
						"onfocus=\"this.value=''\" "+
						"size=5>\n"+
						"</td>\n<datalist id=\""+table+"."+column+"_list\">\n";
					for (String data : q.colMap.keys(table, column)) {
						html += "<option value=\""+data+"\">\n";
					}
					html += "</datalist>\n";
				}
			}
		}
		// complete the form table and start the output table
		html += "</tr>\n</table>\n</form>\n</div>\n<br>\n<div>\n<table>\n<tr>";
		// table headers
		for (String row : q.rowMap.keys()) {
			for (String table : q.rowMap.keys(row)) {
				Table t = q.db.table(table);
				for (String column : q.rowMap.keys(row, table)) {
					if (t.reference(column).equals("")) {
						html += "<th>"+table+"<br>"+column+"</th>\n";
					}
				}
			}
			break;
		}
		html += "</tr>\n";
		// table rows
		for (String row : q.rowMap.keys()) {
			html += "<tr>\n";
			for (String table : q.rowMap.keys(row)) {
				Table t = q.db.table(table);
				for (String column : q.rowMap.keys(row, table)) {
					if (t.reference(column).equals("")) {
						String dataElement = q.rowMap.read(row, table, column);
						html += "<td>"+dataElement+"</td>\n";
						//html += "<td>"+( dataElement!=null ? dataElement : "" )+"</td>\n";
					}
				}
			}
			html += "</tr>\n";
		}
		// complete the output table and complete HTML
		html += "</table>\n</div>\n</body>\n</html>\n";
		System.out.println("["+sessionId+"] Responce: HTML complete");
		
		output( html, "text/html" );
	}

	// SPA
	void outputSPA () {
		output( 
			"<!DOCTYPE html>\r\n"+
			"<html>\r\n"+
			"<head>\r\n"+
			"\r\n"+
			"<title>CanoeDB</title>\r\n"+
			"\r\n"+
			"<style>\r\n"+
			"\r\n"+
			".all {\r\n"+
			"	position: absolute;\r\n"+
			"	top: 0px;\r\n"+
			"	left: 0px;\r\n"+
			"	right: 0px;\r\n"+
			"	bottom: 0px;\r\n"+
			"}\r\n"+
			".banner {\r\n"+
			"	position: absolute;\r\n"+
			"	top: 0px;\r\n"+
			"	left: 0px;\r\n"+
			"	right: 0px;\r\n"+
			"	height: 30px;\r\n"+
			"	background-color: #555E72;\r\n"+
			"}\r\n"+
			".header {\r\n"+
			"	position: absolute;\r\n"+
			"	top: 30px;\r\n"+
			"	left: 0px;\r\n"+
			"	right: 0px;\r\n"+
			"	bottom: 60%;\r\n"+
			"	overflow: auto;\r\n"+
			"	padding: 8px 0 20px 8px;\r\n"+
			"	background-color: #f8f8f8;\r\n"+
			"}\r\n"+
			".rows {\r\n"+
			"	position: absolute;\r\n"+
			"	top: 40%;\r\n"+
			"	left: 0px;\r\n"+
			"	right: 0px;\r\n"+
			"	bottom: 0px;\r\n"+
			"	overflow: auto;\r\n"+
			"	background-color: #f8f8f8;\r\n"+
			"	border-top: 4px solid #555E72;\r\n"+
			"}\r\n"+
			"\r\n"+
			".dbTable {\r\n"+
			"	display: inline-block;\r\n"+
			"	vertical-align : middle;\r\n"+
			"	margin: 4px 8px 4px 8px;\r\n"+
			"	min-width: 250px;\r\n"+
			"	box-shadow: 0 2px 6px 0 rgba(0, 0, 0, 0.3);\r\n"+
			"}\r\n"+
			"\r\n"+
			".column {\r\n"+
			"	width: auto;\r\n"+
			"	padding: 0 8px 0 3px;\r\n"+
			"}\r\n"+
			"\r\n"+
			"\r\n"+
			"div {\r\n"+
			"	margin: 0 0 0 0;\r\n"+
			"	font: 14px arial, sans-serif;\r\n"+
			"}\r\n"+
			"\r\n"+
			"input[type=text] {\r\n"+
			"	font: 14px arial, sans-serif;\r\n"+
			"	padding: 0 4px 0 4px;\r\n"+
			"}\r\n"+
			"input[type=text]:focus {\r\n"+
			"	background-color: lightblue;\r\n"+
			"}\r\n"+
			"input[type=text]:hover {\r\n"+
			"	background-color: lightblue;\r\n"+
			"}\r\n"+
			"\r\n"+
			"table {\r\n"+
			"	border-collapse: collapse;\r\n"+
			"	width: 100%;\r\n"+
			"	font: 14px arial, sans-serif;\r\n"+
			"}\r\n"+
			"\r\n"+
			"th, td {\r\n"+
			"	padding: 4px 16px 4px 16px;\r\n"+
			"	text-align: left;\r\n"+
			"	border: 1px solid #dddddd;\r\n"+
			"	/* border-top: 1px solid #dddddd; */\r\n"+
			"	/* border-left: 1px solid #dddddd; */\r\n"+
			"	/* border-right: 1px solid #dddddd; */\r\n"+
			"}\r\n"+
			"\r\n"+
			"th {\r\n"+
			"	background-color: #555E72;\r\n"+
			"	color: white;\r\n"+
			"	font-size: 16px;\r\n"+
			"	border: 0;\r\n"+
			"}\r\n"+
			"\r\n"+
			".columnTitle {\r\n"+
			"	display: inline-block;\r\n"+
			"	width: auto;\r\n"+
			"	text-align: right;\r\n"+
			"	padding: 0 4px 0 4px;\r\n"+
			"	vertical-align: middle;\r\n"+
			"}\r\n"+
			".enableCheckBox {\r\n"+
			"	display: inline-block;\r\n"+
			"	vertical-align: middle;\r\n"+
			"}\r\n"+
			".transformInput {\r\n"+
			"	display: inline-block;\r\n"+
			"	width: 100px;\r\n"+
			"	vertical-align: middle;\r\n"+
			"	color: #888888;\r\n"+
			"	border: 0;\r\n"+
			"	border-bottom: 1px dotted #888888;\r\n"+
			"	/* margin-left: 4px; */\r\n"+
			"}\r\n"+
			".noTransform {\r\n"+
			"	display: inline-block;\r\n"+
			"	width: 20px;\r\n"+
			"	vertical-align: middle;\r\n"+
			"	color: #888888;\r\n"+
			"	border: 0;\r\n"+
			"	border-bottom: 1px dotted #888888;\r\n"+
			"	/* margin-left: 4px; */\r\n"+
			"}\r\n"+
			".filterInput {\r\n"+
			"	display: inline-block;\r\n"+
			"	width: 120px;\r\n"+
			"	vertical-align: middle;\r\n"+
			"	border: 0;\r\n"+
			"	border-bottom: 1px solid #888888;\r\n"+
			"}\r\n"+
			".tableName {\r\n"+
			"	padding: 0 4px 0 4px;\r\n"+
			"	color: white;\r\n"+
			"	font: 16px arial, sans-serif;	\r\n"+
			"	font-weight: bold;\r\n"+
			"	background-color: #aaaaaa;\r\n"+
			"	width: auto;\r\n"+
			"}\r\n"+
			".tableEnvelope {\r\n"+
			"	padding: 4px 0 8px 0;\r\n"+
			"	background-color: white;\r\n"+
			"}\r\n"+
			".insignia {\r\n"+
			"	position: absolute;\r\n"+
			"	left: 16px;\r\n"+
			"	top: 3px;\r\n"+
			"	width: auto;\r\n"+
			"	height: auto;\r\n"+
			"	color: #aaaaaa;\r\n"+
			"	font: 20px arial, sans-serif;	\r\n"+
			"	font-weight: bold;\r\n"+
			"}\r\n"+
			".databaseFolder {\r\n"+
			"	position: absolute;\r\n"+
			"	left: 125px;\r\n"+
			"	top: 3px;\r\n"+
			"	width: auto;\r\n"+
			"	height: auto;\r\n"+
			"	color: white;\r\n"+
			"	font: 20px arial, sans-serif;	\r\n"+
			"	font-weight: bold;\r\n"+
			"}\r\n"+
			".rightControl {\r\n"+
			"	width: auto;\r\n"+
			"	height: auto;\r\n"+
			"	color: white;\r\n"+
			"	padding: 0 8px 0 8px;\r\n"+
			"	font-weight: bold;\r\n"+
			"}\r\n"+
			".addButton {\r\n"+
			"	font: 30px arial, sans-serif;	\r\n"+
			"	position: absolute;\r\n"+
			"	right: 20px;\r\n"+
			"	top: -2px;\r\n"+
			"}\r\n"+
			".addButton:hover {\r\n"+
			"	background-color: #aaaaaa;\r\n"+
			"	/* opacity: 0.3; */\r\n"+
			"	cursor: default;\r\n"+
			"}\r\n"+
			"\r\n"+
			"\r\n"+
			".output {\r\n"+
			"	background-color: lightblue;	\r\n"+
			"	/* text-shadow: 0 0 12px lightblue, 0 0 12px lightblue, 0 0 12px lightblue; */\r\n"+
			"	/* font-weight: bold; */\r\n"+
			"}\r\n"+
			".input {\r\n"+
			"	background-color: lightgreen;	\r\n"+
			"	/* text-shadow: 0 0 12px lightblue, 0 0 12px lightblue, 0 0 12px lightblue; */\r\n"+
			"	/* font-weight: bold; */\r\n"+
			"}\r\n"+
			"\r\n"+
			".nullCell {\r\n"+
			"	border: 0;\r\n"+
			"}\r\n"+
			".normalCell {\r\n"+
			"	background-color: white;\r\n"+
			"}\r\n"+
			"\r\n"+
			"</style>\r\n"+
			"	\r\n"+
			"</head>\r\n"+
			"<body>\r\n"+
			"\r\n"+
			"<div id=\"root\"></div>\r\n"+
			"\r\n"+
			"<script src=\"https://unpkg.com/react@16/umd/react.development.js\" crossorigin></script>\r\n"+
			"<script src=\"https://unpkg.com/react-dom@16/umd/react-dom.development.js\" crossorigin></script>\r\n"+
			"\r\n"+
			"\r\n"+
			"<script>\r\n"+
			"'use strict';\r\n"+
			"\r\n"+
			"\r\n"+
			"const e = React.createElement;\r\n"+
			"\r\n"+
			"class DataLists extends React.Component {\r\n"+
			"	constructor(props) {\r\n"+
			"		super(props);\r\n"+
			"		//this.state = {...props};\r\n"+
			"	}\r\n"+
			"\r\n"+
			"	render() {\r\n"+
			"		return Object.keys(this.props).map((table) => {\r\n"+
			"			console.log('datalists generated for '+table);\r\n"+
			"			return Object.keys(this.props[table]).map((column) => {\r\n"+
			"				console.log('datalist for '+column);\r\n"+
			"				return e(\r\n"+
			"					'datalist',\r\n"+
			"					{\r\n"+
			"						id: table+'.'+column\r\n"+
			"					},\r\n"+
			"					Object.keys(this.props[table][column]).sort((a,b) => {\r\n"+
			"						return this.props[table][column][b]-this.props[table][column][a]\r\n"+
			"					}).map((op) => {\r\n"+
			"						console.log(op);\r\n"+
			"						return e(\r\n"+
			"							'option',\r\n"+
			"							{\r\n"+
			"								key: table+'.'+column+'.'+op,\r\n"+
			"								value: op,\r\n"+
			"							}\r\n"+
			"						);\r\n"+
			"					})\r\n"+
			"				);\r\n"+
			"			});\r\n"+
			"		});\r\n"+
			"	}\r\n"+
			"}\r\n"+
			"\r\n"+
			"\r\n"+
			"class RowsTable extends React.Component {\r\n"+
			"	constructor(props) {\r\n"+
			"		super(props);\r\n"+
			"		//this.state = {...props};\r\n"+
			"	}\r\n"+
			"\r\n"+
			"	render() {\r\n"+
			"		\r\n"+
			"		let headerArray = [];\r\n"+
			"		let rowsArray = [];\r\n"+
			"		let headerComplete = false;\r\n"+
			"		Object.keys(this.props).sort().forEach(row => {\r\n"+
			"			let rowArray = [];\r\n"+
			"			Object.keys(this.props[row]).sort().forEach(table => {\r\n"+
			"				Object.keys(this.props[row][table]).sort().forEach(column => {\r\n"+
			"					!headerComplete && headerArray.push( column );\r\n"+
			"					rowArray.push( this.props[row][table][column] );\r\n"+
			"				});\r\n"+
			"			});\r\n"+
			"			headerComplete = true;\r\n"+
			"			rowsArray.push( rowArray );\r\n"+
			"		});\r\n"+
			"		console.log( headerArray );\r\n"+
			"		console.log( rowsArray );\r\n"+
			"		\r\n"+
			"		\r\n"+
			"		return e(\r\n"+
			"			'table',\r\n"+
			"			{},\r\n"+
			"			// single header row\r\n"+
			"			e(\r\n"+
			"				'tbody',\r\n"+
			"				{},\r\n"+
			"				e(\r\n"+
			"					'tr',\r\n"+
			"					{},\r\n"+
			"					headerArray.map((header) => {\r\n"+
			"						return e( 'th', {}, header ) \r\n"+
			"					})\r\n"+
			"				),\r\n"+
			"				// many data rows\r\n"+
			"				rowsArray.map((row) => {\r\n"+
			"					return e(\r\n"+
			"						'tr',\r\n"+
			"						{},\r\n"+
			"						// data strings\r\n"+
			"						row.map((data) => {\r\n"+
			"							// javascript automatically resolves null to just an empty string\r\n"+
			"							// so we have to specifically check\r\n"+
			"							return e(\r\n"+
			"								'td',\r\n"+
			"								{\r\n"+
			"									className: (data === null ? 'nullCell' : 'normalCell' )\r\n"+
			"								},\r\n"+
			"								data\r\n"+
			"							);\r\n"+
			"						})\r\n"+
			"					)\r\n"+
			"				})\r\n"+
			"			)\r\n"+
			"		);\r\n"+
			"	}\r\n"+
			"}\r\n"+
			"\r\n"+
			"class ColumnHeader extends React.Component {\r\n"+
			"	constructor(props) {\r\n"+
			"		super(props);\r\n"+
			"		this.state = {...props};\r\n"+
			"\r\n"+
			"		this.inputChange = this.inputChange.bind(this);\r\n"+
			"		this.clearText = this.clearText.bind(this);\r\n"+
			"	}\r\n"+
			"\r\n"+
			"	inputChange(event) {\r\n"+
			"		// assign actual variables, since by the time setState runs the function passed to...\r\n"+
			"		// ...it, I found that sometimes the event.target object reference had already become null.\r\n"+
			"		var targetName = event.target.name;\r\n"+
			"		var targetValue;\r\n"+
			"		// check for type\r\n"+
			"		if ( event.target.type === 'checkbox') {\r\n"+
			"			targetValue = event.target.checked;\r\n"+
			"			// pass a function to setState\r\n"+
			"			this.setState(s => {\r\n"+
			"				s[targetName] = targetValue;\r\n"+
			"				this.state.update(this.state);\r\n"+
			"			});\r\n"+
			"		} else {\r\n"+
			"			targetValue = ( event.target.value ? event.target.value : '' );\r\n"+
			"			// pass a function to setState\r\n"+
			"			this.setState(s => {\r\n"+
			"				s[targetName] = targetValue;\r\n"+
			"				s.enabled = true;\r\n"+
			"				this.state.update(this.state);\r\n"+
			"			});\r\n"+
			"		}\r\n"+
			"	}\r\n"+
			"	\r\n"+
			"	clearText(event) {\r\n"+
			"		var clearTextTarget = event.target.name;\r\n"+
			"		this.setState(s => {\r\n"+
			"			s[clearTextTarget] = '';\r\n"+
			"			this.state.update(this.state);\r\n"+
			"		});\r\n"+
			"	}\r\n"+
			"	\r\n"+
			"	render() {\r\n"+
			"		const {enabled, table, column, options} = this.state;\r\n"+
			"		if (this.state.reference) {\r\n"+
			"			// hide columns that reference other tables\r\n"+
			"			return null;\r\n"+
			"		} else {\r\n"+
			"			return e(\r\n"+
			"				'div',\r\n"+
			"				{\r\n"+
			"					className: 'column'\r\n"+
			"				},\r\n"+
			"				e(\r\n"+
			"					'input',\r\n"+
			"					{\r\n"+
			"						name: 'enabled',\r\n"+
			"						type: 'checkbox',\r\n"+
			"						checked: enabled,\r\n"+
			"						onChange: this.inputChange,\r\n"+
			"						className: 'enableCheckBox'\r\n"+
			"					}\r\n"+
			"				),\r\n"+
			"				e(\r\n"+
			"					'div',\r\n"+
			"					{\r\n"+
			"						className: ( this.state.enabled ? ( this.state.filter ? 'columnTitle input' : 'columnTitle output' ) : 'columnTitle' )\r\n"+
			"					},\r\n"+
			"					this.props.column+':'\r\n"+
			"				),\r\n"+
			"				e(\r\n"+
			"					'input',\r\n"+
			"					{\r\n"+
			"						name: 'filter',\r\n"+
			"						type: 'text',\r\n"+
			"						list: table+'.'+column,\r\n"+
			"						value: this.state.filter,\r\n"+
			"						onFocus: this.clearText,\r\n"+
			"						onChange: this.inputChange,\r\n"+
			"						className: ( enabled && this.state.filter ? 'filterInput input' : 'filterInput' )\r\n"+
			"					}\r\n"+
			"				),\r\n"+
			"				e(\r\n"+
			"					'input',\r\n"+
			"					{\r\n"+
			"						name: 'transform',\r\n"+
			"						type: 'text',\r\n"+
			"						list: 'transforms',\r\n"+
			"						value: this.state.transform,\r\n"+
			"						onChange: this.inputChange,\r\n"+
			"						className: (this.state.transform ? ( this.state.enabled && this.state.filter ? 'transformInput input' : 'transformInput') : 'noTransform' )\r\n"+
			"					}\r\n"+
			"				)\r\n"+
			"			);\r\n"+
			"		}\r\n"+
			"	}\r\n"+
			"}\r\n"+
			"\r\n"+
			"class CanoeDB extends React.Component {\r\n"+
			"	constructor(props) {\r\n"+
			"		super(props);\r\n"+
			"		this.state = {\r\n"+
			"			error: null,\r\n"+
			"			isLoaded: false,\r\n"+
			"			settings: {},\r\n"+
			"			name: '',\r\n"+
			"			structure: {},\r\n"+
			"			columns: {},\r\n"+
			"			rows: {},\r\n"+
			"			logic: 'and'\r\n"+
			"		}\r\n"+
			"		\r\n"+
			"		this.update = this.update.bind(this);\r\n"+
			"		this.transmit = this.transmit.bind(this);\r\n"+
			"	}\r\n"+
			"	\r\n"+
			"	update(newSettings = {}) {\r\n"+
			"		let table = newSettings.table;\r\n"+
			"		let column = newSettings.column;\r\n"+
			"		this.setState(s => {\r\n"+
			"			// vivify if necessary\r\n"+
			"			if (!s.settings.hasOwnProperty(table)) s.settings[table] = {};\r\n"+
			"			if (!s.settings[table].hasOwnProperty(column)) s.settings[table][column] = {};\r\n"+
			"			// overlay newSettings onto settings\r\n"+
			"			Object.assign( s.settings[table][column], newSettings );\r\n"+
			"			// trigger a new transmission\r\n"+
			"			this.transmit();\r\n"+
			"		});\r\n"+
			"	}\r\n"+
			"	\r\n"+
			"	transmit( writeMode ) {\r\n"+
			"		// start new transmission\r\n"+
			"		let settings = this.state.settings;\r\n"+
			"		console.log(settings);\r\n"+
			"		\r\n"+
			"		let query = [];\r\n"+
			"		Object.keys(settings).sort().forEach(table => {\r\n"+
			"			Object.keys(settings[table]).sort().forEach(column => {\r\n"+
			"				let thisCol = settings[table][column];\r\n"+
			"				if (thisCol.enabled) {\r\n"+
			"					query.push( table+'.'+column+( thisCol.transform ? '.'+thisCol.transform : '' )+'='+thisCol.filter );\r\n"+
			"				}\r\n"+
			"			});\r\n"+
			"		});\r\n"+
			"		var url = '/json'+\r\n"+
			"			'/'+this.state.logic+\r\n"+
			"			( writeMode ? '/write' : '' )+\r\n"+
			"			( query.length>0 ? '?'+query.join('&') : '' );\r\n"+
			"		console.log('GET '+url);\r\n"+
			"		fetch( url, {\r\n"+
			"			// mode: 'no-cors' // 'cors' by default\r\n"+
			"			mode: 'cors'\r\n"+
			"		})\r\n"+
			"		.then(res => res.json())\r\n"+
			"		.then(\r\n"+
			"			(result) => {\r\n"+
			"				console.log(result.structure);\r\n"+
			"				console.log(result.columns);\r\n"+
			"				console.log(result.rows)\r\n"+
			"				this.setState({\r\n"+
			"					isLoaded: true,\r\n"+
			"					name: result.name,\r\n"+
			"					structure: result.structure,\r\n"+
			"					columns: result.columns,\r\n"+
			"					rows: result.rows\r\n"+
			"				});\r\n"+
			"			},\r\n"+
			"			// Note: it's important to handle errors here\r\n"+
			"			// instead of a catch() block so that we don't swallow\r\n"+
			"			// exceptions from actual bugs in components.\r\n"+
			"			(error) => {\r\n"+
			"				this.setState({\r\n"+
			"					isLoaded: true,\r\n"+
			"					error\r\n"+
			"				});\r\n"+
			"			}\r\n"+
			"		)\r\n"+
			"	}\r\n"+
			"	\r\n"+
			"	componentDidMount() {\r\n"+
			"		// blank transmission to initialize\r\n"+
			"		this.transmit();\r\n"+
			"	}\r\n"+
			"	\r\n"+
			"	render() {\r\n"+
			"		\r\n"+
			"		console.log('Rendering Interface...');	\r\n"+
			"		\r\n"+
			"		const { error, isLoaded, name, structure, rows, columns, settings } = this.state;\r\n"+
			"		// console.log('Columns:');\r\n"+
			"		// console.log(columns);\r\n"+
			"		if (error) {\r\n"+
			"			return e(\r\n"+
			"				'div',\r\n"+
			"				{},\r\n"+
			"				e( 'p', {}, error.message ),\r\n"+
			"				e( 'p', {}, JSON.stringify( structure ) )\r\n"+
			"			);\r\n"+
			"		} else if (!isLoaded) {\r\n"+
			"			return e(\r\n"+
			"				'div',\r\n"+
			"				{},\r\n"+
			"				'Loading...'\r\n"+
			"			);\r\n"+
			"		} else {\r\n"+
			"			return e(\r\n"+
			"				'div',\r\n"+
			"				{\r\n"+
			"					className: 'all'\r\n"+
			"				},\r\n"+
			"				// banner DIV\r\n"+
			"				e(\r\n"+
			"					'div',\r\n"+
			"					{\r\n"+
			"						className: 'banner'\r\n"+
			"					},\r\n"+
			"					e( 'div', {className: 'insignia'}, 'CanoeDB' ),\r\n"+
			"					e( 'div', {className: 'databaseFolder'}, name ),\r\n"+
			"					e(\r\n"+
			"						'div',\r\n"+
			"						{\r\n"+
			"							className: 'rightControl addButton',\r\n"+
			"							onClick: ()=>{this.transmit(true)}\r\n"+
			"						},\r\n"+
			"						'+'\r\n"+
			"					)\r\n"+
			"				),\r\n"+
			"				e(\r\n"+
			"					// header DIV\r\n"+
			"					'div',\r\n"+
			"					{\r\n"+
			"						className: 'header'\r\n"+
			"					},\r\n"+
			"					// loop through tables\r\n"+
			"					Object.keys(structure).sort().map((table) => {\r\n"+
			"						// table DIV\r\n"+
			"						return e(\r\n"+
			"							'div',\r\n"+
			"							{\r\n"+
			"								key: table,\r\n"+
			"								className: 'dbTable'\r\n"+
			"							},\r\n"+
			"							e( 'div', {className: 'tableName'}, table ),\r\n"+
			"							// loop through columns\r\n"+
			"							e(\r\n"+
			"								'div',\r\n"+
			"								{\r\n"+
			"									className: 'tableEnvelope'\r\n"+
			"								},\r\n"+
			"								Object.keys(structure[table]).sort().map((column) => {\r\n"+
			"									let props_obj = Object.assign(\r\n"+
			"										// default values\r\n"+
			"										{\r\n"+
			"											key: table+column,\r\n"+
			"											table: table,\r\n"+
			"											column: column,\r\n"+
			"											filter: '',\r\n"+
			"											transform: '',\r\n"+
			"											reference: '',\r\n"+
			"											enabled: false,\r\n"+
			"											update: this.update\r\n"+
			"										},\r\n"+
			"										// structure returned from the database\r\n"+
			"										structure[table][column],\r\n"+
			"										// any settings produced by the interface\r\n"+
			"										(\r\n"+
			"											settings.hasOwnProperty(table) && settings[table].hasOwnProperty(column) ?\r\n"+
			"											settings[table][column] : {}\r\n"+
			"										)\r\n"+
			"									);\r\n"+
			"									// column Element\r\n"+
			"									return e( ColumnHeader, props_obj );\r\n"+
			"								})\r\n"+
			"							)\r\n"+
			"						);\r\n"+
			"					}),\r\n"+
			"					e( DataLists, columns ),\r\n"+
			"					e(\r\n"+
			"						'datalist',\r\n"+
			"						{\r\n"+
			"							id: 'transforms'\r\n"+
			"						},\r\n"+
			"						e( 'option', {value:'First'} ),\r\n"+
			"						e( 'option', {value:'Last'} ),\r\n"+
			"						e( 'option', {value:'TimeStamp'} ),\r\n"+
			"						e( 'option', {value:'TransmitBase64'} ),\r\n"+
			"						e( 'option', {value:'StoreBase64'} )\r\n"+
			"					)\r\n"+
			"				),\r\n"+
			"				e(\r\n"+
			"					// build rows display\r\n"+
			"					'div',\r\n"+
			"					{\r\n"+
			"						className: 'rows'\r\n"+
			"					},\r\n"+
			"					//e( 'p', {}, JSON.stringify( rows ) )\r\n"+
			"					e( RowsTable, this.state.rows )\r\n"+
			"				)\r\n"+
			"			);\r\n"+
			"		}\r\n"+
			"	}\r\n"+
			"}\r\n"+
			"\r\n"+
			"\r\n"+
			"const domContainer = document.querySelector('#root');\r\n"+
			"ReactDOM.render(e(CanoeDB), domContainer);\r\n"+
			"\r\n"+
			"</script>\r\n"+
			"\r\n"+
			"\r\n"+
			"</body>\r\n"+
			"</html>"
			
			, "text/html" );
	}

}
