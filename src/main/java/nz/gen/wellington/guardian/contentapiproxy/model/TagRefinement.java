package nz.gen.wellington.guardian.contentapiproxy.model;

public class TagRefinement implements Refinement {

	private Tag tag;
	
	public TagRefinement(Tag tag) {
		this.tag = tag;
	}

	public Tag getTag() {
		return tag;
	}
	
}
