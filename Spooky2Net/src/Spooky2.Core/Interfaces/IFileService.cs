namespace Spooky2.Core.Interfaces;

public interface IFileService
{
    bool Exists(string path);
    bool IsDirectory(string path);
    void DeleteFile(string path);
    void DeleteDirectory(string path, bool recursive = false);
    string[] GetFiles(string directory, string pattern = "*");
    string[] GetDirectories(string directory);
    void CreateDirectory(string path);
    Task<string> ReadAllText(string path);
    Task<string> ReadAllText(string path, System.Text.Encoding encoding);
    Task WriteAllText(string path, string content);
    Task AppendText(string path, string content);
}
