/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.WMSMapContent;

public class ScaleLineDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER =
        org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.responses");

    public int scaleWidthPercent = 100;

    private float fontSize = 10;
    private float dpi = 25.4f / 0.28f; /// OGC Spec for SLD
    private float strokeWidth = 2;

    private Color bgcolor = Color.WHITE;
    private Color fgcolor = Color.BLACK;

    private Boolean transparent = Boolean.FALSE;

	private Unit topUnit = null;
	private Unit bottomUnit = null;
	private Map<String, Unit> units = new HashMap<>();

	public ScaleLineDecoration() {

		// Set up all systems
		units.put("metric", new Unit(new UnitType("METER", "m", 39.3701, 100000), new UnitType("KILOMETER", "km", 39370.1, 0)));
		units.put("imperial", new Unit(new UnitType("FEET", "ft", 12.0, 20000), new UnitType("YARD", "yd", 36.0, 100000), new UnitType("MILE", "mi", 63360.0, 0)));
		units.put("oldimperial", new Unit(new UnitType("FEET", "ft", 12.0, 20000), new UnitType("MILE", "mi", 63360.0, 0)));
		units.put("neutical", new Unit(new UnitType("NEUTICAL MILE", "nmi", 72913.4, 0)));
		units.put("decimal", new Unit(new UnitType("DECIMAL DEGREES", "dd", 4374754.0, 0)));

		// Default values
		topUnit = units.get("imperial");
		bottomUnit = units.get("metric");
	}

    public void loadOptions(Map<String, String> options) {
    	if (options.get("fontsize") != null) {
    		try {
    			this.fontSize = Float.parseFloat(options.get("fontsize"));
    		} catch (Exception e) {
    			LOGGER.log(Level.WARNING, "'fontsize' must be a float.", e);
    		}
    	}

    	if (options.get("dpi") != null) {
    		try {
    			this.dpi = Float.parseFloat(options.get("dpi"));
    		} catch (Exception e) {
    			LOGGER.log(Level.WARNING, "'dpi' must be a float.", e);
    		}
    	}

    	if (options.get("strokewidth") != null) {
    		try {
    			this.strokeWidth = Float.parseFloat(options.get("strokeWidth"));
    		} catch (Exception e) {
    			LOGGER.log(Level.WARNING, "'strokewidth' must be a float.", e);
    		}
    	}

        Color tmp = MapDecorationLayout.parseColor(options.get("bgcolor"));
        if (tmp != null) bgcolor = tmp;

        tmp = MapDecorationLayout.parseColor(options.get("fgcolor"));
        if (tmp != null) fgcolor = tmp;

    	// Creates a rectangle only if is defined, if not is "transparent" like Google Maps
    	if (options.get("transparent") != null) {
    		try {
    			this.transparent = Boolean.parseBoolean(options.get("transparent"));
    		} catch (Exception e) {
    			LOGGER.log(Level.WARNING, "'transparent' must be a boolean.", e);
    		}
    	}

    	//Only for backwards compatibility
		if (options.get("measurement-system") != null) {
			try {
				LOGGER.log(Level.INFO, options.get("measurement-system"));
				String type = options.get("measurement-system");

    	    	switch (type) {
    	    		case "metric":
    	    	    	bottomUnit = units.get("metric");
    	    	    	topUnit = null;
    	    	    	break;
    	    		case "imperial":
    	    	    	bottomUnit = units.get("oldimperial");
    	    	    	topUnit = null;
    	    	    	break;
    	    		case "both":
    	    	    	bottomUnit = units.get("metric");
    	    	    	topUnit = units.get("oldimperial");
    	    	    	break;
    	    		default:
    	    	    	throw new Exception("Wrong input parameter");
    	    	}

			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "'measurement-system' must be one of 'metric', 'imperial' or 'both', for others use the 'topunit' and 'bottomunit' instead.", e);
            }
		}

    	if (options.get("scalewidthpercent") != null) {
    		try {
    	    	LOGGER.log(Level.INFO, options.get("scalewidthpercent"));
    	    	this.scaleWidthPercent = Integer.parseInt((options.get("scalewidthpercent")));
    		} catch (Exception e) {
    	    	LOGGER.log(Level.WARNING, "'scalewidthpercent' must be an integer.", e);
    		}
    	}

    	if (options.get("topunit") != null) {
    		try {
    	    	LOGGER.log(Level.INFO, options.get("topunit"));
    	    	Unit topu = units.get(options.get("topunit"));
    	    	if (topu != null) {
    	    		topUnit = topu;
    	    	} else {
    	    		throw new Exception("Wrong input parameter");
    	    	}
    		} catch (Exception e) {
    	    	LOGGER.log(Level.WARNING, "'topunit' given was not found or was not a String.", e);
    		}
    	}

    	if (options.get("bottomunit") != null) {
    		try {
    	    	LOGGER.log(Level.INFO, options.get("bottomunit"));
    	    	Unit topu = units.get(options.get("bottomunit"));
    	    	if (topu != null) {
    	    		topUnit = topu;
    	    	} else {
    	    		throw new Exception("Wrong input parameter");
    	    	}
    		} catch (Exception e) {
    	    	LOGGER.log(Level.WARNING, "'bottomunit' given was not found or was not a String.", e);
    	    }
        }

	}

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) {
    	FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
    	return new Dimension(
            scaleWidthPercent, 8 + (metrics.getHeight() + metrics.getDescent()) * 2
        );
    }

    public int getBarLength(double maxLength) {
    	int digits = (int) (Math.log(maxLength) / Math.log(10));
    	double pow10 = Math.pow(10, digits);

    	// Find first character
    	int firstCharacter = (int)(maxLength / pow10);

    	int barLength;
    	if (firstCharacter > 5) {
    		barLength = 5;
    	} else if (firstCharacter > 2) {
    		barLength = 2;
    	} else {
    		barLength = 1;
    	}

    	return (int)(barLength * pow10);
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent)
    throws Exception {
    	Color oldColor = g2d.getColor();
    	Stroke oldStroke = g2d.getStroke();
    	Font oldFont = g2d.getFont();
    	Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

    	// Set the font size.
    	g2d.setFont(oldFont.deriveFont(this.fontSize));

        double scaleDenominator = mapContent.getScaleDenominator(true);

    	double normalizedScale = (scaleDenominator > 1.0)
            ? (1.0 / scaleDenominator)
            : scaleDenominator;


    	int maxWidth = scaleWidthPercent;

    	if (maxWidth > paintArea.getWidth()) {
    		maxWidth = (int)paintArea.getWidth();
    	}

        maxWidth = maxWidth - 6;

    	int topSize = 0;
    	int bottomSize = 0;
    	BufferedImage scaleLineTop = new BufferedImage((int) paintArea.getWidth(), (int) paintArea.getHeight(), BufferedImage.TYPE_INT_ARGB);
    	BufferedImage scaleLineBottom = new BufferedImage((int) paintArea.getWidth(), (int) paintArea.getHeight(), BufferedImage.TYPE_INT_ARGB);

    	if (topUnit != null) {
    		topSize = makeScaleLine(true, topUnit, maxWidth, normalizedScale, paintArea, oldAntialias, g2d, scaleLineTop, bottomUnit == null);
    	}
    	if (bottomUnit != null) {
    		bottomSize = makeScaleLine(false, bottomUnit, maxWidth, normalizedScale, paintArea, oldAntialias, g2d, scaleLineBottom, topUnit == null);
    	}

    	int centerY = (int) paintArea.getCenterY();

    	int scaleLineMaxWidth = Math.max(topSize, bottomSize);

    	int leftX = (int)paintArea.getMinX() + ((int) paintArea.getWidth() - scaleLineMaxWidth) / 2;

    	FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
    	int prongHeight = metrics.getHeight() + metrics.getDescent();

		// Turn off antialiasing for border
    	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    	// Creates a rectangle only if is defined, if not is "transparent" like Google Maps
    	if (!this.transparent) {
    		Rectangle frame = new Rectangle(
    			leftX-24, centerY - prongHeight-8,
    			scaleLineMaxWidth+48, (prongHeight * 2)+16
    		);

    		// Fill the rectangle
    		g2d.setColor(bgcolor);
    		g2d.fill(frame);

    		// Draw the border
    		frame.height -= 1;
    		frame.width -= 1;
    		g2d.setStroke(new BasicStroke(1));
    		g2d.setColor(fgcolor);
    		g2d.draw(frame);
    	}

    	// Draw the bars on top of the background
		g2d.drawImage(scaleLineTop, 0, 0, null);
		g2d.drawImage(scaleLineBottom, 0, 0, null);

		// Restore old settings
    	g2d.setColor(oldColor);
    	g2d.setStroke(oldStroke);
    	g2d.setFont(oldFont);
    }

	private int makeScaleLine(boolean top, Unit unit, double scaleDenominator, double normalizedScale, Rectangle paintArea, Object oldAntialias, Graphics2D parentGraphics, BufferedImage image, boolean onlyOne) {

    	double baseNumber = units.get("metric").getBaseType().getInchesPerUnit();

    	double resolution = 1 / (normalizedScale * baseNumber * this.dpi);

    	int maxWidth = scaleWidthPercent;

    	if (maxWidth > paintArea.getWidth()) {
    		maxWidth = (int) paintArea.getWidth();
    	}

    	maxWidth = maxWidth - 6;

    	double maxSizeData = maxWidth * baseNumber * resolution;

    	UnitType currentUnitType = unit.getUnitType(maxSizeData);

    	double topMax = maxSizeData / currentUnitType.getInchesPerUnit();

    	int textLenght = this.getBarLength(topMax);

    	topMax = (textLenght / baseNumber) * currentUnitType.getInchesPerUnit();//INCHES_PER_UNIT.get(curMapUnits) * INCHES_PER_UNIT.get(unitAbbrevation);

    	// leftX = how much more to the right than leftX
    	int rightX = (int) (topMax / resolution);

    	int centerY = (int) paintArea.getCenterY();

    	int leftX = (int) paintArea.getMinX() + ((int) paintArea.getWidth() - rightX) / 2;

    	FontMetrics metrics = parentGraphics.getFontMetrics(parentGraphics.getFont());
    	int prongHeight = metrics.getHeight() + metrics.getDescent();

    	Graphics2D g2d = image.createGraphics();

    	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    	paintScaleLine(top, g2d, leftX, rightX, prongHeight, centerY, metrics, oldAntialias, textLenght, currentUnitType, onlyOne);

    	return rightX;
	}

	private void paintScaleLine(boolean top, Graphics2D g2d, int leftX, int rightX, int prongHeight, int centerY, FontMetrics metrics, Object oldAntialias, int topRounded, UnitType currentUnitType, boolean onlyOne) {
    	int smallProngHeight = prongHeight / 4;
    	int centerX = (leftX + leftX + rightX) / 2;
    	g2d.setColor(fgcolor);
    	g2d.setStroke(new BasicStroke(this.strokeWidth));


    	// Same start if top or !top
    	int verticalStart = onlyOne ? centerY - smallProngHeight : centerY;
    	int verticalEnd;

    	if (onlyOne) {
    		verticalEnd = centerY + smallProngHeight;
    	} else if (top) {
    		verticalEnd = centerY - (smallProngHeight * 2);
    	} else {
    		verticalEnd = centerY + (smallProngHeight * 2);
    	}
    	g2d.drawLine(leftX, verticalStart, leftX, verticalEnd);


    	// Right vertical bar
    	g2d.drawLine(leftX + rightX, verticalStart, leftX + rightX, verticalEnd);

    	if (scaleWidthPercent >= 200) {
    		g2d.drawLine(centerX, verticalStart, centerX, verticalEnd);
    	}

    	// Draw horizontal line
    	g2d.drawLine(leftX, centerY, leftX + rightX, centerY);

    	//Antialias text if enabled
    	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

    	if (onlyOne) {
    		// Draw text on top middle if there is only one
    		g2d.drawString(currentUnitType.getLongName(),
    	    		centerX - (metrics.stringWidth(currentUnitType.getLongName()) / 2),
    	    		centerY - prongHeight + metrics.getAscent() + 2
    		);
    	}

    	int numbersY;
    	if (onlyOne || !top) {
    		numbersY = centerY + (prongHeight + metrics.getDescent() + 1);
    	} else {
    		numbersY = centerY - (prongHeight - metrics.getDescent() - 2);
    	}

    	String unitAbbrevationAddedToNumbers = "";
    	if (!onlyOne) {
    		unitAbbrevationAddedToNumbers = currentUnitType.getShortName();
    	}

    	String leftNumber = "0" + unitAbbrevationAddedToNumbers;
    	g2d.drawString(leftNumber,
    	    	leftX - (metrics.stringWidth(leftNumber) / 2),
    	    	numbersY
    	);

    	if (scaleWidthPercent >= 200) {
    		String middleText = new DecimalFormat("#.#").format((double) topRounded / 2) + unitAbbrevationAddedToNumbers;
    		g2d.drawString(middleText,
    	    		centerX - (metrics.stringWidth(middleText) / 2),
    	    		numbersY
    		);
    	}

    	String rightText = String.format("%d", topRounded) + unitAbbrevationAddedToNumbers;
    	g2d.drawString(rightText,
    	    	(rightX + leftX - (metrics.stringWidth(rightText) / 2)),
    	    	numbersY
    	);
	}

	private class Unit {
    	UnitType[] unitTypes;

    	public Unit(UnitType... unitTypes) {
    		this.unitTypes = unitTypes;
    	}

    	public UnitType getBaseType() {
    		return unitTypes[0];
    	}

    	public UnitType getUnitType(double maxSizeData) {
    		//Gets the first unit which corresponds to maxSizeData(number of units if basetype is used)
    		for (UnitType unitType : unitTypes) {
    	    	if (unitType.getMaxUnitNumber() > maxSizeData || unitType.getMaxUnitNumber() == 0) {
    	    		return unitType;
    	    	}
    		}
    		return null;
    	}
	}

	private class UnitType {
    	private String longName;
    	private String shortName;
    	private double inchesPerUnit;
    	//0 for none, a number when to change to the next, default is <100000
    	private int maxUnitNumber;

    	public UnitType(String longName, String shortName, double inchesPerUnit, int maxUnitNumber) {
    		this.longName = longName;
    		this.shortName = shortName;
    		this.inchesPerUnit = inchesPerUnit;
    		this.maxUnitNumber = maxUnitNumber;
    	}

    	public String getLongName() {
    		return longName;
    	}

    	public String getShortName() {
    		return shortName;
    	}

    	public Double getInchesPerUnit() {
    		return inchesPerUnit;
    	}

    	public int getMaxUnitNumber() {
    		return maxUnitNumber;
    	}
	}
}
