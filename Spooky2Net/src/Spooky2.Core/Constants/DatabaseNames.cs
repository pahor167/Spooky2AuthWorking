namespace Spooky2.Core.Constants;

public static class DatabaseNames
{
    public const string RIFE = "RIFE"; // Royal Rife frequency database
    public const string CAFL = "CAFL"; // Consolidated Annotated Frequency List
    public const string XTRA = "XTRA"; // Extra/supplementary frequencies
    public const string CUST = "CUST"; // Custom user-defined database
    public const string DNA = "DNA"; // DNA frequency calculations
    public const string HC = "HC"; // Healing Codes
    public const string ALT = "ALT"; // Alternative medicine frequencies
    public const string BIO = "BIO"; // Biological frequencies
    public const string PROV = "PROV"; // Provisional/unverified frequencies
    public const string MW = "MW"; // Molecular Weight derived frequencies
    public const string VEGA = "VEGA"; // VEGA test frequencies
    public const string ETDFL = "ETDFL"; // Extended Target Disease Frequency List
    public const string BFB = "BFB"; // Biofeedback scan frequencies
    public const string KHZ = "KHZ"; // Kilohertz range frequencies
    public const string SD = "SD"; // Spooky Database
    public const string RUSS = "RUSS"; // Russian frequency research
    public const string RRM = "RRM"; // Remote Rife Machine frequencies
    public const string CUST1 = "CUST1"; // Custom user-defined database 1
    public const string CUST2 = "CUST2"; // Custom user-defined database 2
    public const string CUST3 = "CUST3"; // Custom user-defined database 3
    public const string CUST4 = "CUST4"; // Custom user-defined database 4

    public static readonly string[] AllDatabases =
    [
        RIFE, CAFL, XTRA, CUST, DNA, HC, ALT, BIO,
        PROV, MW, VEGA, ETDFL, BFB, KHZ, SD, RUSS, RRM,
        CUST1, CUST2, CUST3, CUST4
    ];
}
