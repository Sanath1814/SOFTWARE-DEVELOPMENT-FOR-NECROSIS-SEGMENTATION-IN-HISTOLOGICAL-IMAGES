import numpy as np
import matplotlib.pyplot as plt
from sklearn.cluster import DBSCAN
import shapely.geometry as geometry
from shapely.geometry import Polygon, MultiPolygon
from shapely.ops import unary_union, polygonize
from scipy.spatial import Delaunay
import math
from shapely.geometry import Point
import sys


def add_Edge(edges, edge_Points, coords, i, j):
    """
    Add an edge to a set of edges if it doesn't already exist, and store the
    corresponding points.

    Parameters:
    - edges (set of tuples): A set of edges already added.
    - edge_Points (list of numpy arrays): A list of coordinates of the points of
    the edges added.
    - coords (numpy array): A numpy array containing the coordinates of all
    points.
    - i (int): The index of the first point of the edge.
    - j (int): The index of the second point of the edge.
    """
    if (i, j) in edges or (j, i) in edges:
        return
    edges.add((i, j))
    edge_Points.append(coords[[i, j]])


def alpha_Shape(points, alpha):
    """
    Compute the alpha shape (concave hull) of a set of points.

    Parameters:
    - points (list of shapely.geometry.Point): A list of shapely Point objects
    representing the set of points.
    - alpha (float): The alpha parameter controlling the desired detail of the
    concave hull.

    Returns:
    - tuple: A tuple containing the concave hull (shapely.geometry.Polygon or
    shapely.geometry.MultiPolygon) and the edge points (list of numpy arrays).
    """

    # If there are less than 4 points, compute the convex hull using MultiPoint
    # and return it with an empty list of edge points
    if len(points) < 4:
        return geometry.MultiPoint(list(points)).convex_hull, []

    coords = np.array([point.coords[0] for point in points])

    # Perform Delaunay triangulation on the points
    tri = Delaunay(coords)

    edges = set()
    edge_Points = []

    # Iterate over each triangle in the Delaunay triangulation
    for ia, ib, ic in tri.simplices:
        pa = coords[ia]
        pb = coords[ib]
        pc = coords[ic]

        # Calculate the length of each side of the triangle
        a = math.sqrt((pa[0] - pb[0]) ** 2 + (pa[1] - pb[1]) ** 2)
        b = math.sqrt((pb[0] - pc[0]) ** 2 + (pb[1] - pc[1]) ** 2)
        c = math.sqrt((pc[0] - pa[0]) ** 2 + (pc[1] - pa[1]) ** 2)

        # Calculate the circumcircle radius and check if it is less than the
        # inverse of alpha
        s = (a + b + c) / 2.0
        area = math.sqrt(s * (s - a) * (s - b) * (s - c))
        circum_r = a * b * c / (4.0 * area)

        # If the circumcircle radius is less than 1/alpha, add the edges to the
        # set and edge points to the list
        if circum_r < 1.0 / alpha:
            add_Edge(edges, edge_Points, coords, ia, ib)
            add_Edge(edges, edge_Points, coords, ib, ic)
            add_Edge(edges, edge_Points, coords, ic, ia)

    # Create a MultiLineString from the edge points and polygonize it to form
    # polygons
    multi_Line = geometry.MultiLineString(edge_Points)
    polygons = list(polygonize(multi_Line))

    # Return the union of all polygons (concave hull) and the list of edge
    # points
    return unary_union(polygons), edge_Points


def find_Best_Alpha(points, start_Alpha, end_Alpha, step):
    """
    Finds the best alpha value to compute the alpha shape (concave hull) of a
    set of points.

    Parameters:
    - points (list of shapely.geometry.Point): A list of shapely Point objects
    representing the set of points.
    - start_Alpha (float): The starting value of the alpha parameter.
    - end_Alpha (float): The ending value of the alpha parameter.
    - step (float): The decremental step value to adjust the alpha parameter.

    Returns:
    - tuple: A tuple containing the best alpha value (float) and the
    corresponding concave hull (shapely.geometry.Polygon or None).
             If no suitable alpha value is found, returns (-1, None).
    """
    alpha = start_Alpha
    best_Alpha = None
    best_Concave_Hull = None

    # Loop until alpha is greater than or equal to end_Alpha
    while alpha >= end_Alpha:
        # Compute the concave hull and edge points using alpha_Shape function
        concave_Hull, edge_Points = alpha_Shape(points, alpha)

        # Check if the computed concave hull is a valid Polygon and is not None
        if isinstance(concave_Hull,
                      Polygon) and not concave_Hull.is_empty and points_in_polygon(
                points, concave_Hull):
            best_Alpha = alpha
            best_Concave_Hull = concave_Hull
            break

        # Check if the computed concave hull is a MultiPolygon with a single
        # geometry and is not None
        elif isinstance(concave_Hull, MultiPolygon):
            if len(concave_Hull.geoms) == 1 and not \
                    concave_Hull.geoms[0].is_empty and points_in_polygon(points,
                                                                         concave_Hull):
                best_Alpha = alpha
                best_Concave_Hull = concave_Hull.geoms[0]
                break

        alpha -= step

    # Check if a valid concave hull was found
    if best_Concave_Hull is not None:
        return best_Alpha, best_Concave_Hull
    else:
        # Return (-1, Convexe form) indicating no suitable alpha value was found
        return -1, geometry.MultiPoint(points).convex_hull


def plot_Clusters_With_Hulls(points, distance, color, label):
    """
    Plots clusters of points with their corresponding concave hulls.

    Parameters:
    - points (numpy array): A numpy array containing the coordinates of the
    points.
    - distance (float): The maximum distance between two points for them to be
    considered as neighbors in DBSCAN.
    - color (str): The color used to plot the points and their concave hulls.
    - label (str): The label for the plotted points.

    Returns:
    - float: The total area covered by the concave hulls of all clusters.
    """

    if (len(points) == 0):
        return 0, None

    # Cluster the points using DBSCAN
    clustering = DBSCAN(eps=distance, min_samples=1).fit(points)
    labels = clustering.labels_

    # Get the unique cluster labels
    unique_Labels = set(labels)
    total_Area = 0

    # Store concave hulls for further processing
    concave_hulls = []

    # Iterate over each unique cluster
    for label_idx, label_Value in enumerate(unique_Labels):
        # Filter the points belonging to the current cluster
        cluster_Points = points[labels == label_Value]
        cluster_Points_Geom = [geometry.Point(p) for p in cluster_Points]

        # At least 3 points are needed to define a polygon
        if len(cluster_Points) >= 3:
            best_Alpha, concave_Hull = find_Best_Alpha(cluster_Points_Geom, 1,
                                                       0.1, 0.1)

            if concave_Hull is not None:
                concave_hulls.append(concave_Hull)

                if display_Graph == "1":
                    x, y = concave_Hull.exterior.xy
                    plt.fill(x, y, color, alpha=0.3)

                area = concave_Hull.area
                total_Area += area
            else:
                if display_Graph == "1":
                    plt.plot(cluster_Points[:, 0], cluster_Points[:, 1], 'o',
                             color=color, markersize=1)
        else:
            if display_Graph == "1":
                plt.plot(cluster_Points[:, 0], cluster_Points[:, 1], 'o',
                         color=color, markersize=1)

    plt.plot(points[:, 0], points[:, 1], 'o', color=color, label=f'{label}',
             markersize=1)

    # Return the total area covered by concave hulls and the concave hulls
    # themselves
    return total_Area, concave_hulls


def points_in_polygon(points, polygon):
    """
    Checks if all points in the set 'points' are within the polygon 'polygon'.
    The vertices and edges of the polygon are considered as included.

    Args:
    - points: List of tuples representing the coordinates of the points to
    check.
    - polygon: List of tuples representing the vertices of the polygon in order.

    Returns:
    - True if all points are inside the polygon, False otherwise.
    """
    # Create the Polygon object from the list of polygon vertices
    poly = Polygon(polygon)

    # Check each point
    for point_coords in points:
        point = Point(point_coords)
        if not poly.intersects(point):
            return False

    return True


def parse_string_to_arrays(input_string):
    """
    Parses an input string into two numpy arrays representing viable and
    necrotic cells.

    Args:
    - input_string: A string containing the coordinates of viable and
    necrotic cells,
                    with each type of cells on a separate line.

    Returns:
    - tuple: A tuple containing two numpy arrays (viable cells, necrotic cells).
             Returns (None, None) if the input string is not in the expected
             format.
    """
    # Split the string into lines
    lines = input_string.strip().split('\n')

    if len(lines) == 2:

        # Convert each line into a list of lists of floats
        viable_cells = np.array(eval(lines[0]))
        necrotic_cells = np.array(eval(lines[1]))

        return viable_cells, necrotic_cells

    else:
        return None, None


# We recover data from the arguments
with open(sys.argv[1], 'r') as file:
    args = [line.strip() for line in file.readlines()]

string_Cellules_Viables = args[0]
cellules_Viables = np.array(eval(string_Cellules_Viables)).reshape(-1, 2)
cellules_Viables[:, 1] *= -1  # Echelle QuPath inversé pour les Y

string_Cellules_Necrosees = args[1]
cellules_Necrosees = np.array(eval(string_Cellules_Necrosees)).reshape(-1, 2)
cellules_Necrosees[:, 1] *= -1  # Echelle QuPath inversé pour les Y

distance_Max_Pour_Etre_Voisin = int(args[2])
display_Graph = args[3]
file_Path = args[4]

# cellules_Viables, cellules_Necrosees =
# parse_string_to_arrays(gravity_Centers_Cells)

# Create a new figure
if (display_Graph == "1"):
    plt.figure()

# Plot clusters and concave hulls for necrotic cells
aire_necrosees, concave_hulls_necrosees = plot_Clusters_With_Hulls(
    cellules_Necrosees, distance_Max_Pour_Etre_Voisin, 'blue', 'Necrotic cells')

# Plot clusters and concave hulls for viable cells
aire_viables, concave_hulls_viables = plot_Clusters_With_Hulls(cellules_Viables,
                                                               distance_Max_Pour_Etre_Voisin,
                                                               'red',
                                                               'Viable cells')

if concave_hulls_necrosees is not None and concave_hulls_viables is not None:

    # Calculate the intersections of the concave hulls
    for hull_necrose in concave_hulls_necrosees:
        for hull_viable in concave_hulls_viables:
            if hull_necrose.intersects(hull_viable):
                intersection = hull_necrose.intersection(hull_viable)
                intersection_area = intersection.area

                # If viable area is within necrotic area
                if hull_necrose.contains(hull_viable):
                    aire_necrosees -= intersection_area

                # If necrotic area is within viable area
                elif hull_viable.contains(hull_necrose):
                    aire_viables -= intersection_area

# Calculate the necrosis rate
if aire_necrosees + aire_viables == 0:
    print("No zone with at least 3 cells.")
else:
    taux_Necrose = (aire_viables / (aire_necrosees + aire_viables)) * 100

    # Print the total areas and necrosis rate
    print(
        f'Total area of necrotic cells = {aire_necrosees:.2f} µm²'
        f'\nTotal area of viable cells = {aire_viables:.2f} µm²'
        f'\nNecrosis rate = {taux_Necrose:.2f} %')

# Display the plot
if display_Graph == "1":
    plt.xlabel('X')
    plt.ylabel('Y')
    plt.legend()
    plt.savefig(file_Path)
