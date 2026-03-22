using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IDatabaseService
{
    Task<List<DatabaseEntry>> LoadDatabase(string databaseName);
    Task<List<DatabaseEntry>> SearchDatabase(string searchText, IEnumerable<string> databases);
    Task<DatabaseEntry?> GetEntry(string name, string database);
    Task RefreshDatabase();
}
