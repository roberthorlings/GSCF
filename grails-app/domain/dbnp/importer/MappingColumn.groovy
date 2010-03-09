package dbnp.importer

/**
* name: column name
* type: GCSF field type
* entity: GSCF entity class
* property: GSCF field property
* celltype: Excel cell type
* index: column index
* value: column value (optional, normally only name is used)
*/
class MappingColumn {

	String name
	dbnp.studycapturing.TemplateFieldType templatefieldtype
	Class entity
	String property
	Integer celltype
	Integer index
	String value

    static constraints = {
	    name(unique: true)
    }

    String toString() {
	return "Name:" + name + "/Type:" + templatefieldtype + "/Entity:" + entity + "/Property:" + property + "/Celltype:" + celltype + "/Index:" + index + "/Value:" + value
    }
}
